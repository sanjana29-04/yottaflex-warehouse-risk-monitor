package com.warehouse.riskmonitor;

import com.warehouse.riskmonitor.entity.*;
import com.warehouse.riskmonitor.entity.enums.ReadingStatus;
import com.warehouse.riskmonitor.generator.SyntheticDataGenerator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class SyntheticDataGeneratorTest {

    @Autowired
    private SyntheticDataGenerator generator;

    @Test
    @DisplayName("Verify full scale generation produces exact required entity counts and valid relationships")
    public void testFullScaleExactCountsAndRelationships() {
        long seed = 20261022L;
        SyntheticDataGenerator.GeneratedDataset dataset = generator.generate("full", seed, false);

        // Exact Challenge Entity Counts
        assertEquals(10, dataset.warehouses.size(), "Must generate exactly 10 warehouses");
        assertEquals(100, dataset.zones.size(), "Must generate exactly 100 zones (10 wh * 10 zones)");
        assertEquals(500, dataset.sensors.size(), "Must generate exactly 500 sensors (100 zones * 5 sensors)");
        assertEquals(10000, dataset.inventoryLots.size(), "Must generate exactly 10,000 inventory lots");
        assertEquals(1000000, dataset.readings.size(), "Must generate exactly 1,000,000 sensor readings (500 sensors * 2,000 readings)");

        // Foreign Key Integrity
        Set<String> warehouseIds = dataset.warehouses.stream().map(Warehouse::getId).collect(Collectors.toSet());
        Set<String> zoneIds = dataset.zones.stream().map(Zone::getId).collect(Collectors.toSet());
        Set<String> sensorIds = dataset.sensors.stream().map(Sensor::getId).collect(Collectors.toSet());
        Set<String> reqIds = dataset.storageRequirements.stream().map(StorageRequirement::getId).collect(Collectors.toSet());

        assertEquals(10, warehouseIds.size(), "Warehouse IDs must be unique");
        assertEquals(100, zoneIds.size(), "Zone IDs must be unique");
        assertEquals(500, sensorIds.size(), "Sensor IDs must be unique");

        for (Zone z : dataset.zones) {
            assertNotNull(z.getWarehouse(), "Zone must belong to a warehouse");
            assertTrue(warehouseIds.contains(z.getWarehouse().getId()), "Zone warehouse must exist");
        }

        for (Sensor s : dataset.sensors) {
            assertNotNull(s.getZone(), "Sensor must belong to a zone");
            assertTrue(zoneIds.contains(s.getZone().getId()), "Sensor zone must exist");
        }

        for (InventoryLot lot : dataset.inventoryLots) {
            assertNotNull(lot.getCurrentZone(), "Lot must belong to a zone");
            assertTrue(zoneIds.contains(lot.getCurrentZone().getId()), "Lot zone must exist");
            assertNotNull(lot.getStorageRequirement(), "Lot must have storage requirement");
            assertTrue(reqIds.contains(lot.getStorageRequirement().getId()), "Requirement must exist");
        }

        // Anomaly Ground-Truth & Normal Readings Presence
        long normalReadings = dataset.readings.stream().filter(r -> !r.getIsAnomaly()).count();
        long anomalyReadings = dataset.readings.stream().filter(SensorReading::getIsAnomaly).count();
        assertTrue(normalReadings > 0, "Must contain normal sensor readings");
        assertTrue(anomalyReadings > 0, "Must contain abnormal readings with anomaly ground-truth");
        assertEquals(1000000, normalReadings + anomalyReadings);
        assertFalse(dataset.groundTruthAnomalies.isEmpty(), "Ground truth anomaly list must not be empty");
    }

    @Test
    @DisplayName("Verify deterministic reproducibility with fixed random seed 20261022")
    public void testDeterministicReproducibility() {
        long seed = 20261022L;

        // Run 1
        SyntheticDataGenerator.GeneratedDataset run1 = generator.generate("sample", seed, false);

        // Run 2
        SyntheticDataGenerator.GeneratedDataset run2 = generator.generate("sample", seed, false);

        // Verify entity counts identical
        assertEquals(run1.warehouses.size(), run2.warehouses.size());
        assertEquals(run1.zones.size(), run2.zones.size());
        assertEquals(run1.sensors.size(), run2.sensors.size());
        assertEquals(run1.inventoryLots.size(), run2.inventoryLots.size());
        assertEquals(run1.readings.size(), run2.readings.size());
        assertEquals(run1.doorEvents.size(), run2.doorEvents.size());

        // Verify exact ID and value matches
        for (int i = 0; i < run1.warehouses.size(); i++) {
            assertEquals(run1.warehouses.get(i).getId(), run2.warehouses.get(i).getId());
            assertEquals(run1.warehouses.get(i).getCapacitySqm(), run2.warehouses.get(i).getCapacitySqm());
        }

        for (int i = 0; i < run1.readings.size(); i++) {
            assertEquals(run1.readings.get(i).getId(), run2.readings.get(i).getId());
            assertEquals(run1.readings.get(i).getValue(), run2.readings.get(i).getValue(), 0.0001);
            assertEquals(run1.readings.get(i).getStatus(), run2.readings.get(i).getStatus());
        }
    }
}
