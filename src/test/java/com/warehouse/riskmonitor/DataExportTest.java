package com.warehouse.riskmonitor;

import com.warehouse.riskmonitor.generator.SyntheticDataGenerator;
import com.warehouse.riskmonitor.service.ExportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class DataExportTest {

    @Autowired
    private SyntheticDataGenerator generator;

    @Autowired
    private ExportService exportService;

    @BeforeEach
    public void setupData() {
        // Generate sample dataset in database
        generator.generate("sample", 20261022L, true);
    }

    @Test
    @DisplayName("Verify CSV exports for multiple entities contain expected headers and records")
    public void testCsvExports() {
        // Warehouses CSV
        String csvWarehouses = exportService.exportEntityToCsv("warehouses");
        assertNotNull(csvWarehouses);
        assertTrue(csvWarehouses.startsWith("id,name,location,capacity_sqm,total_zones,status"));
        assertTrue(csvWarehouses.contains("WH-001"));

        // Zones CSV
        String csvZones = exportService.exportEntityToCsv("zones");
        assertNotNull(csvZones);
        assertTrue(csvZones.startsWith("id,warehouse_id,name,zone_type,capacity_lots"));
        assertTrue(csvZones.contains("ZONE-WH-001"));

        // Sensors CSV
        String csvSensors = exportService.exportEntityToCsv("sensors");
        assertNotNull(csvSensors);
        assertTrue(csvSensors.startsWith("id,zone_id,sensor_type,status,model"));
        assertTrue(csvSensors.contains("SNS-"));

        // Inventory Lots CSV
        String csvLots = exportService.exportEntityToCsv("inventory_lots");
        assertNotNull(csvLots);
        assertTrue(csvLots.startsWith("id,product_name,sku,category,quantity"));
        assertTrue(csvLots.contains("LOT-"));

        // Sensor Readings CSV
        String csvReadings = exportService.exportEntityToCsv("sensor_readings");
        assertNotNull(csvReadings);
        assertTrue(csvReadings.startsWith("id,sensor_id,zone_id,reading_type,reading_value"));
        assertTrue(csvReadings.contains("RDG-"));

        // Door Events CSV
        String csvDoors = exportService.exportEntityToCsv("door_events");
        assertNotNull(csvDoors);
        assertTrue(csvDoors.startsWith("id,zone_id,door_name,opened_at,closed_at"));
    }

    @Test
    @DisplayName("Verify JSON exports for entities return valid JSON arrays")
    public void testJsonExports() {
        // JSON Warehouses
        String jsonWarehouses = exportService.exportEntityToJson("warehouses");
        assertNotNull(jsonWarehouses);
        assertTrue(jsonWarehouses.startsWith("[") && jsonWarehouses.endsWith("]"));
        assertTrue(jsonWarehouses.contains("WH-001"));

        // JSON Zones
        String jsonZones = exportService.exportEntityToJson("zones");
        assertNotNull(jsonZones);
        assertTrue(jsonZones.startsWith("[") && jsonZones.endsWith("]"));
        assertTrue(jsonZones.contains("ZONE-WH-001"));

        // JSON Alerts
        String jsonAlerts = exportService.exportEntityToJson("alerts");
        assertNotNull(jsonAlerts);
        assertTrue(jsonAlerts.startsWith("[") && jsonAlerts.endsWith("]"));
    }

    @Test
    @DisplayName("Verify SQL export generates valid INSERT statements")
    public void testSqlExports() {
        // Warehouses SQL
        String sqlWarehouses = exportService.exportEntityToSql("warehouses");
        assertNotNull(sqlWarehouses);
        assertTrue(sqlWarehouses.contains("INSERT INTO warehouses"));
        assertTrue(sqlWarehouses.contains("WH-001"));

        // Zones SQL
        String sqlZones = exportService.exportEntityToSql("zones");
        assertNotNull(sqlZones);
        assertTrue(sqlZones.contains("INSERT INTO zones"));

        // Sensors SQL
        String sqlSensors = exportService.exportEntityToSql("sensors");
        assertNotNull(sqlSensors);
        assertTrue(sqlSensors.contains("INSERT INTO sensors"));
    }

    @Test
    @DisplayName("Verify unsupported entity or format throws meaningful exception")
    public void testUnknownEntityThrowsException() {
        assertThrows(RuntimeException.class, () -> exportService.exportEntityToCsv("non_existent_table"));
        assertThrows(RuntimeException.class, () -> exportService.exportEntityToJson("non_existent_table"));
        assertThrows(RuntimeException.class, () -> exportService.exportEntityToSql("non_existent_table"));
    }
}
