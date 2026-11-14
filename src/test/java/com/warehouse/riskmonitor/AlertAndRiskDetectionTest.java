package com.warehouse.riskmonitor;

import com.warehouse.riskmonitor.entity.*;
import com.warehouse.riskmonitor.entity.enums.*;
import com.warehouse.riskmonitor.repository.StorageRequirementRepository;
import com.warehouse.riskmonitor.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class AlertAndRiskDetectionTest {

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
    private SpatiotemporalRiskEngine spatiotemporalRiskEngine;

    @Test
    public void testAlertLifecycleAndLotRiskQuarantine() {
        Warehouse wh = new Warehouse("WH-TEST-04", "Risk Center", "Frankfurt, DE", 40000, 4, WarehouseStatus.ACTIVE);
        warehouseService.saveWarehouse(wh);

        Zone zone = new Zone("ZONE-WH-TEST-04-001", wh, "Vaccine Cold Storage", ZoneType.FROZEN, 500, -25.0, -18.0, 30.0, 60.0, 6);
        zoneService.saveZone(zone, wh.getId());

        StorageRequirement req = new StorageRequirement("REQ-TEST-VAC", "Vaccine Standard", ProductCategory.VACCINES, -20.0, -25.0, -18.0, 40.0, 20.0, 60.0, 120);
        storageRequirementRepository.save(req);

        InventoryLot lot = new InventoryLot("LOT-TEST-001", "COVID Vaccine Batch 9", "SKU-VAC-99", ProductCategory.VACCINES,
                200.0, UnitOfMeasure.PALLETS, req, zone, LocalDateTime.now().minusDays(5), LocalDateTime.now().plusYears(1), LotStatus.STORED);
        inventoryLotService.saveLot(lot, req.getId(), zone.getId());

        // 1. Create alert for thermal excursion
        Alert alert = alertService.createAlert(zone.getId(), AlertType.HIGH_TEMPERATURE, RiskLevel.CRITICAL, -5.0, -18.0,
                "Critical temperature rise: -5.0°C exceeds -18.0°C threshold");
        assertNotNull(alert);
        assertEquals(AlertStatus.OPEN, alert.getStatus());
        assertEquals(RiskLevel.CRITICAL, alert.getRiskLevel());

        // 2. Evaluate spatiotemporal impact
        int affectedLots = spatiotemporalRiskEngine.evaluateSpatiotemporalExposure(alert.getId());
        assertTrue(affectedLots > 0);

        InventoryLot evaluatedLot = inventoryLotService.getLotById("LOT-TEST-001");
        assertTrue(evaluatedLot.getThermalStressScore() > 0.0);
        assertEquals(LotStatus.QUARANTINED, evaluatedLot.getStatus());

        // 3. Acknowledge and resolve alert
        Alert acked = alertService.acknowledgeAlert(alert.getId(), "Lead Operator", "Inspecting cooling compressor");
        assertEquals(AlertStatus.ACKNOWLEDGED, acked.getStatus());

        Alert resolved = alertService.resolveAlert(alert.getId(), "Quality Auditor", "Secondary compressor started. Safe temperature restored.");
        assertEquals(AlertStatus.RESOLVED, resolved.getStatus());
        assertNotNull(resolved.getResolvedAt());
    }
}

