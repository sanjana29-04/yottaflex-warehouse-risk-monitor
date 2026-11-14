package com.warehouse.riskmonitor;

import com.warehouse.riskmonitor.dto.EvaluationMetricsDto;
import com.warehouse.riskmonitor.entity.GroundTruthAnomaly;
import com.warehouse.riskmonitor.entity.InventoryLot;
import com.warehouse.riskmonitor.entity.SensorReading;
import com.warehouse.riskmonitor.entity.enums.AnomalyType;
import com.warehouse.riskmonitor.entity.enums.ProductCategory;
import com.warehouse.riskmonitor.entity.enums.ZoneType;
import com.warehouse.riskmonitor.generator.SyntheticDataGenerator;
import com.warehouse.riskmonitor.repository.GroundTruthAnomalyRepository;
import com.warehouse.riskmonitor.repository.InventoryLotRepository;
import com.warehouse.riskmonitor.repository.SensorReadingRepository;
import com.warehouse.riskmonitor.repository.SensorZoneMappingRepository;
import com.warehouse.riskmonitor.service.EvaluationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class NegativeScenariosAndEvaluationTest {

    @Autowired
    private SyntheticDataGenerator generator;

    @Autowired
    private GroundTruthAnomalyRepository anomalyRepository;

    @Autowired
    private InventoryLotRepository lotRepository;

    @Autowired
    private SensorZoneMappingRepository mappingRepository;

    @Autowired
    private SensorReadingRepository readingRepository;

    @Autowired
    private EvaluationService evaluationService;

    @BeforeEach
    public void setupData() {
        // Generate sample dataset with real edge cases in database
        generator.generate("sample", 20261022L, true);
    }

    @Test
    @DisplayName("Verify all 4 required negative scenarios are explicitly labelled in ground truth AND physically exist in database")
    public void testFourRequiredNegativeScenariosLabelledAndInjected() {
        List<GroundTruthAnomaly> anomalies = anomalyRepository.findAll();
        assertFalse(anomalies.isEmpty(), "Ground truth anomaly repository must contain labelled negative scenarios");

        // Scenario 1: Lot assigned to incompatible zone
        Optional<GroundTruthAnomaly> gta1 = anomalies.stream()
                .filter(a -> a.getAnomalyType() == AnomalyType.LOT_ASSIGNED_INCOMPATIBLE_ZONE)
                .findFirst();
        assertTrue(gta1.isPresent(), "Edge Case 1: LOT_ASSIGNED_INCOMPATIBLE_ZONE must be present");
        assertNotNull(gta1.get().getEntityId());
        assertEquals("InventoryLot", gta1.get().getEntityType());
        assertEquals("QUARANTINE_AND_REALLOCATE", gta1.get().getExpectedAction());

        // Verify physical entity is actually stored in incompatible zone
        InventoryLot lot = lotRepository.findById(gta1.get().getEntityId()).orElseThrow();
        assertEquals(ProductCategory.VACCINES, lot.getCategory(), "Lot must be a Vaccine");
        assertEquals(ZoneType.AMBIENT, lot.getCurrentZone().getZoneType(), "Vaccine must be allocated to Ambient zone");

        // Scenario 2: Sensor mapped to multiple zones
        Optional<GroundTruthAnomaly> gta2 = anomalies.stream()
                .filter(a -> a.getAnomalyType() == AnomalyType.SENSOR_MAPPED_MULTIPLE_ZONES)
                .findFirst();
        assertTrue(gta2.isPresent(), "Edge Case 2: SENSOR_MAPPED_MULTIPLE_ZONES must be present");
        assertNotNull(gta2.get().getEntityId());
        assertEquals("Sensor", gta2.get().getEntityType());
        assertEquals("CORRECT_SENSOR_TOPOLOGY", gta2.get().getExpectedAction());

        // Verify physical conflict exists in sensor_zone_mappings table
        List<Object[]> multipleActive = mappingRepository.findSensorsMappedToMultipleActiveZones();
        assertFalse(multipleActive.isEmpty(), "Physical sensor_zone_mappings conflict must exist");
        assertEquals(gta2.get().getEntityId(), multipleActive.get(0)[0], "Conflicting sensor ID must match GTA-002");

        // Scenario 3: Missing sensor reading gap
        Optional<GroundTruthAnomaly> gta3 = anomalies.stream()
                .filter(a -> a.getAnomalyType() == AnomalyType.UNREPORTED_SENSOR_READING_GAP)
                .findFirst();
        assertTrue(gta3.isPresent(), "Edge Case 3: UNREPORTED_SENSOR_READING_GAP must be present");
        assertNotNull(gta3.get().getEntityId());
        assertEquals("SensorReading", gta3.get().getEntityType());
        assertEquals("FLAG_SENSOR_COMMUNICATION_FAULT", gta3.get().getExpectedAction());

        // Verify physical gap exists in sensor_readings
        List<SensorReading> gapReadings = readingRepository.findBySensorAndTimeRange(
                gta3.get().getEntityId(),
                LocalDateTime.of(2026, 11, 1, 8, 0),
                LocalDateTime.of(2026, 11, 1, 8, 50));
        assertTrue(gapReadings.isEmpty(), "Telemetry blackout interval must have zero readings in database");

        // Scenario 4: Alarm not linked to affected inventory lots
        Optional<GroundTruthAnomaly> gta4 = anomalies.stream()
                .filter(a -> a.getAnomalyType() == AnomalyType.ALARM_NOT_LINKED_TO_AFFECTED_LOT)
                .findFirst();
        assertTrue(gta4.isPresent(), "Edge Case 4: ALARM_NOT_LINKED_TO_AFFECTED_LOT must be present");
        assertNotNull(gta4.get().getEntityId());
        assertEquals("Alarm", gta4.get().getEntityType());
        assertEquals("RECONCILE_SPATIOTEMPORAL_IMPACT", gta4.get().getExpectedAction());
    }

    @Test
    @DisplayName("Verify evaluation metrics computation against real detected anomalies")
    public void testEvaluationMetricsCalculation() {
        EvaluationMetricsDto metrics = evaluationService.evaluateAgainstGroundTruth();

        assertNotNull(metrics);
        assertTrue(metrics.getTruePositives() >= 4, "All 4 negative scenarios must be detected as True Positives");
        assertEquals(0, metrics.getFalsePositives(), "False Positives should be 0 on valid benchmark");
        assertEquals(0, metrics.getFalseNegatives(), "False Negatives should be 0 on fully detected benchmark");
        assertEquals(1.0, metrics.getPrecision(), 0.001, "Precision should be 1.0 (100%)");
        assertEquals(1.0, metrics.getRecall(), 0.001, "Recall should be 1.0 (100%)");
        assertEquals(1.0, metrics.getF1Score(), 0.001, "F1 score should be 1.0 (100%)");
        assertTrue(metrics.getMeanDetectionLatencyMinutes() > 0.0, "Mean detection latency must be greater than 0");
        assertFalse(metrics.getAnomalies().isEmpty(), "Anomalies list must be returned in metrics DTO");
    }
}
