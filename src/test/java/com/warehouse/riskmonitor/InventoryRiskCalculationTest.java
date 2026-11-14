package com.warehouse.riskmonitor;

import com.warehouse.riskmonitor.entity.*;
import com.warehouse.riskmonitor.entity.enums.*;
import com.warehouse.riskmonitor.repository.*;
import com.warehouse.riskmonitor.service.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class InventoryRiskCalculationTest {

    @Autowired
    private WarehouseService warehouseService;

    @Autowired
    private ZoneService zoneService;

    @Autowired
    private AlertService alertService;

    @Autowired
    private InventoryLotService inventoryLotService;

    @Autowired
    private StorageRequirementRepository storageRequirementRepository;

    @Autowired
    private LotZoneAssignmentRepository lotZoneAssignmentRepository;

    @Autowired
    private AlarmLotImpactRepository alarmLotImpactRepository;

    @Autowired
    private AlertRepository alertRepository;

    @Autowired
    private SpatiotemporalRiskEngine spatiotemporalRiskEngine;

    @Test
    @DisplayName("Verify thermal stress score calculation triggers quarantine for high exposure")
    public void testThermalStressHighExposureQuarantine() {
        Warehouse wh = new Warehouse("WH-RISK-01", "Cold Hub Risk", "Frankfurt, DE", 30000, 2, WarehouseStatus.ACTIVE);
        warehouseService.saveWarehouse(wh);

        Zone zone = new Zone("ZONE-WH-RISK-01-001", wh, "Vaccine Cold Vault", ZoneType.FROZEN, 400, -25.0, -18.0, 30.0, 60.0, 6);
        zoneService.saveZone(zone, wh.getId());

        StorageRequirement req = new StorageRequirement("REQ-RISK-01", "Biologics Standard", ProductCategory.VACCINES, -20.0, -25.0, -18.0, 40.0, 20.0, 60.0, 120);
        storageRequirementRepository.save(req);

        LocalDateTime now = LocalDateTime.now();

        // Lot present in zone from 2 hours ago (saveLot automatically creates the LotZoneAssignment)
        InventoryLot lot = new InventoryLot("LOT-RISK-001", "Monoclonal Antibody Batch", "SKU-BIO-01", ProductCategory.VACCINES,
                150.0, UnitOfMeasure.BOXES, req, zone, now.minusHours(2), now.plusMonths(6), LotStatus.STORED);
        inventoryLotService.saveLot(lot, req.getId(), zone.getId());

        // Alert: Temperature rose to -8.0°C (threshold -18.0°C, so deltaT = 10.0°C)
        Alert alert = alertService.createAlert(zone.getId(), AlertType.HIGH_TEMPERATURE, RiskLevel.CRITICAL, -8.0, -18.0,
                "Severe temperature breach in cold vault");

        // Set alert duration: 1 hour excursion
        alert.setTriggeredAt(now.minusMinutes(60));
        alert.setResolvedAt(now);
        alertRepository.save(alert);

        // Run spatiotemporal risk calculation
        int affectedCount = spatiotemporalRiskEngine.evaluateSpatiotemporalExposure(alert.getId());
        assertEquals(1, affectedCount);

        InventoryLot updatedLot = inventoryLotService.getLotById("LOT-RISK-001");
        assertNotNull(updatedLot);

        // DeltaT = 10.0, Duration = 60 mins -> stressScore = 60 * 10 = 600.0 > 50.0 threshold
        assertTrue(updatedLot.getThermalStressScore() >= 50.0, "Thermal stress score should exceed quarantine threshold");
        assertEquals(LotStatus.QUARANTINED, updatedLot.getStatus(), "Lot must be automatically quarantined");
        assertTrue(updatedLot.getExposureDurationMinutes() >= 55.0, "Exposure duration must reflect overlapping dwell time");

        List<AlarmLotImpact> impacts = alarmLotImpactRepository.findByLot_Id(lot.getId());
        assertFalse(impacts.isEmpty());
        assertEquals("IMMEDIATE_QUARANTINE", impacts.get(0).getActionRecommended());
    }

    @Test
    @DisplayName("Verify moderate exposure triggers quality inspection recommendation without quarantine")
    public void testModerateExposureQualityInspectionRequired() {
        Warehouse wh = new Warehouse("WH-RISK-02", "Pharma Center", "Rotterdam, NL", 25000, 2, WarehouseStatus.ACTIVE);
        warehouseService.saveWarehouse(wh);

        Zone zone = new Zone("ZONE-WH-RISK-02-001", wh, "Controlled Ambient", ZoneType.COLD_STORAGE, 300, 2.0, 8.0, 40.0, 60.0, 5);
        zoneService.saveZone(zone, wh.getId());

        StorageRequirement req = new StorageRequirement("REQ-RISK-02", "Refrigerated Pharma", ProductCategory.PHARMACEUTICAL, 5.0, 2.0, 8.0, 50.0, 30.0, 70.0, 60);
        storageRequirementRepository.save(req);

        LocalDateTime now = LocalDateTime.now();

        // saveLot automatically creates the LotZoneAssignment
        InventoryLot lot = new InventoryLot("LOT-RISK-002", "Insulin Batch 2", "SKU-INS-02", ProductCategory.PHARMACEUTICAL,
                100.0, UnitOfMeasure.PALLETS, req, zone, now.minusHours(1), now.plusMonths(12), LotStatus.STORED);
        inventoryLotService.saveLot(lot, req.getId(), zone.getId());

        // Moderate excursion: 9.0°C (threshold 8.0°C, deltaT = 1.0°C) for 20 minutes -> stressScore = 20 * 1 = 20.0 (between 15 and 50)
        Alert alert = alertService.createAlert(zone.getId(), AlertType.HIGH_TEMPERATURE, RiskLevel.MEDIUM, 9.0, 8.0,
                "Mild temperature drift above threshold");
        alert.setTriggeredAt(now.minusMinutes(20));
        alert.setResolvedAt(now);
        alertRepository.save(alert);

        int affectedCount = spatiotemporalRiskEngine.evaluateSpatiotemporalExposure(alert.getId());
        assertEquals(1, affectedCount);

        InventoryLot updatedLot = inventoryLotService.getLotById("LOT-RISK-002");
        assertTrue(updatedLot.getThermalStressScore() > 15.0 && updatedLot.getThermalStressScore() <= 50.0);
        assertEquals(LotStatus.STORED, updatedLot.getStatus(), "Lot should not be quarantined for moderate exposure");

        List<AlarmLotImpact> impacts = alarmLotImpactRepository.findByLot_Id(lot.getId());
        assertEquals("QUALITY_INSPECTION_REQUIRED", impacts.get(0).getActionRecommended());
    }

    @Test
    @DisplayName("Verify non-overlapping lot dwell is not affected by alert")
    public void testNonOverlappingLotNotAffected() {
        Warehouse wh = new Warehouse("WH-RISK-03", "Logistics Base", "Chicago, IL", 20000, 1, WarehouseStatus.ACTIVE);
        warehouseService.saveWarehouse(wh);

        Zone zone = new Zone("ZONE-WH-RISK-03-001", wh, "Buffer Zone", ZoneType.BUFFER, 200, 10.0, 20.0, 40.0, 70.0, 4);
        zoneService.saveZone(zone, wh.getId());

        StorageRequirement req = new StorageRequirement("REQ-RISK-03", "Ambient Standard", ProductCategory.GENERAL_MERCHANDISE, 18.0, 10.0, 25.0, 50.0, 20.0, 80.0, 240);
        storageRequirementRepository.save(req);

        LocalDateTime now = LocalDateTime.now();

        // Lot was saved, and we set its dwell to ended 2 days ago
        InventoryLot lot = new InventoryLot("LOT-RISK-003", "Bulk Packaging", "SKU-PKG-03", ProductCategory.GENERAL_MERCHANDISE,
                50.0, UnitOfMeasure.BOXES, req, zone, now.minusDays(3), now.plusYears(2), LotStatus.STORED);
        inventoryLotService.saveLot(lot, req.getId(), zone.getId());

        // Update the assignment created by saveLot so egress was 2 days ago
        List<LotZoneAssignment> assignments = lotZoneAssignmentRepository.findAll();
        for (LotZoneAssignment a : assignments) {
            if (a.getLot() != null && "LOT-RISK-003".equals(a.getLot().getId())) {
                a.setIngressTimestamp(now.minusDays(3));
                a.setEgressTimestamp(now.minusDays(2));
                lotZoneAssignmentRepository.save(a);
            }
        }

        // Alert triggered today (no temporal overlap)
        Alert alert = alertService.createAlert(zone.getId(), AlertType.HIGH_TEMPERATURE, RiskLevel.MEDIUM, 28.0, 20.0,
                "Recent temperature peak");
        alert.setTriggeredAt(now.minusHours(1));
        alert.setResolvedAt(now);

        int affectedCount = spatiotemporalRiskEngine.evaluateSpatiotemporalExposure(alert.getId());
        assertEquals(0, affectedCount, "No lots should be affected because dwell does not overlap alert interval");

        InventoryLot unaffectedLot = inventoryLotService.getLotById("LOT-RISK-003");
        assertEquals(0.0, unaffectedLot.getThermalStressScore());
        assertEquals(LotStatus.STORED, unaffectedLot.getStatus());
    }
}
