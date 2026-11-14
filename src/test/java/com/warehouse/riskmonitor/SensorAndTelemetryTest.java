package com.warehouse.riskmonitor;

import com.warehouse.riskmonitor.dto.TimeSeriesPointDto;
import com.warehouse.riskmonitor.entity.Sensor;
import com.warehouse.riskmonitor.entity.SensorReading;
import com.warehouse.riskmonitor.entity.Warehouse;
import com.warehouse.riskmonitor.entity.Zone;
import com.warehouse.riskmonitor.entity.enums.*;
import com.warehouse.riskmonitor.service.SensorService;
import com.warehouse.riskmonitor.service.TelemetryService;
import com.warehouse.riskmonitor.service.WarehouseService;
import com.warehouse.riskmonitor.service.ZoneService;
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
public class SensorAndTelemetryTest {

    @Autowired
    private WarehouseService warehouseService;

    @Autowired
    private ZoneService zoneService;

    @Autowired
    private SensorService sensorService;

    @Autowired
    private TelemetryService telemetryService;

    @Test
    @DisplayName("Verify temperature excursion detection (high and low boundaries)")
    public void testTemperatureExcursionDetection() {
        Warehouse wh = new Warehouse("WH-TEST-03", "Pharma Hub", "Atlanta, GA", 15000, 2, WarehouseStatus.ACTIVE);
        warehouseService.saveWarehouse(wh);

        Zone zone = new Zone("ZONE-WH-TEST-03-001", wh, "Chilled Section", ZoneType.COLD_STORAGE, 300, 2.0, 8.0, 45.0, 75.0, 5);
        zoneService.saveZone(zone, wh.getId());

        Sensor tempSensor = new Sensor("SNS-WH-TEST-03-001-TEMP-01", zone, SensorType.TEMPERATURE, SensorStatus.ACTIVE, "MOD-TEMP-TEST", 0.0);
        sensorService.saveSensor(tempSensor, zone.getId());

        // 1. Normal reading (4.5°C within [2.0 .. 8.0])
        SensorReading normalRdg = telemetryService.recordReading(tempSensor.getId(), 4.5, LocalDateTime.now());
        assertEquals(ReadingStatus.NORMAL, normalRdg.getStatus());
        assertFalse(normalRdg.getIsAnomaly());
        assertEquals("°C", normalRdg.getUnit());

        // 2. High temperature excursion reading (12.8°C > 8.0°C)
        SensorReading highExcursion = telemetryService.recordReading(tempSensor.getId(), 12.8, LocalDateTime.now());
        assertEquals(ReadingStatus.EXCURSION, highExcursion.getStatus());
        assertTrue(highExcursion.getIsAnomaly());

        // 3. Low temperature excursion reading (-1.5°C < 2.0°C)
        SensorReading lowExcursion = telemetryService.recordReading(tempSensor.getId(), -1.5, LocalDateTime.now());
        assertEquals(ReadingStatus.EXCURSION, lowExcursion.getStatus());
        assertTrue(lowExcursion.getIsAnomaly());

        // 4. Verify latest reading value on sensor
        Sensor updatedSensor = sensorService.getSensorById(tempSensor.getId());
        assertEquals(-1.5, updatedSensor.getLatestReadingValue());
    }

    @Test
    @DisplayName("Verify humidity anomaly detection for out-of-range boundaries")
    public void testHumidityExcursionDetection() {
        Warehouse wh = new Warehouse("WH-TEST-03B", "Pharma Hub B", "Atlanta, GA", 15000, 2, WarehouseStatus.ACTIVE);
        warehouseService.saveWarehouse(wh);

        Zone zone = new Zone("ZONE-WH-TEST-03B-001", wh, "Ambient Dry Goods", ZoneType.AMBIENT, 300, 15.0, 25.0, 30.0, 60.0, 10);
        zoneService.saveZone(zone, wh.getId());

        Sensor humSensor = new Sensor("SNS-WH-TEST-03B-001-HUM-01", zone, SensorType.HUMIDITY, SensorStatus.ACTIVE, "MOD-HUM-TEST", 0.0);
        sensorService.saveSensor(humSensor, zone.getId());

        // 1. Normal humidity (45.0% within [30.0 .. 60.0])
        SensorReading normalRdg = telemetryService.recordReading(humSensor.getId(), 45.0, LocalDateTime.now());
        assertEquals(ReadingStatus.NORMAL, normalRdg.getStatus());
        assertFalse(normalRdg.getIsAnomaly());
        assertEquals("%", normalRdg.getUnit());

        // 2. High humidity excursion (82.5% > 60.0%)
        SensorReading highHum = telemetryService.recordReading(humSensor.getId(), 82.5, LocalDateTime.now());
        assertEquals(ReadingStatus.EXCURSION, highHum.getStatus());
        assertTrue(highHum.getIsAnomaly());

        // 3. Low humidity excursion (18.0% < 30.0%)
        SensorReading lowHum = telemetryService.recordReading(humSensor.getId(), 18.0, LocalDateTime.now());
        assertEquals(ReadingStatus.EXCURSION, lowHum.getStatus());
        assertTrue(lowHum.getIsAnomaly());
    }

    @Test
    @DisplayName("Verify occupancy outlier anomaly detection and time series retrieval")
    public void testOccupancyAndTimeSeries() {
        Warehouse wh = new Warehouse("WH-TEST-03C", "Depot C", "Atlanta, GA", 15000, 2, WarehouseStatus.ACTIVE);
        warehouseService.saveWarehouse(wh);

        Zone zone = new Zone("ZONE-WH-TEST-03C-001", wh, "Vault Zone", ZoneType.COLD_STORAGE, 200, 2.0, 8.0, 40.0, 70.0, 4);
        zoneService.saveZone(zone, wh.getId());

        Sensor occSensor = new Sensor("SNS-WH-TEST-03C-001-OCC-01", zone, SensorType.OCCUPANCY, SensorStatus.ACTIVE, "MOD-OCC-TEST", 0.0);
        sensorService.saveSensor(occSensor, zone.getId());

        // Normal occupancy (2 <= max 4)
        SensorReading normalOcc = telemetryService.recordReading(occSensor.getId(), 2.0, LocalDateTime.now().minusMinutes(10));
        assertEquals(ReadingStatus.NORMAL, normalOcc.getStatus());
        assertFalse(normalOcc.getIsAnomaly());

        // Over-capacity occupancy outlier (8 > max 4)
        SensorReading outlierOcc = telemetryService.recordReading(occSensor.getId(), 8.0, LocalDateTime.now().minusMinutes(5));
        assertEquals(ReadingStatus.OUTLIER, outlierOcc.getStatus());
        assertTrue(outlierOcc.getIsAnomaly());

        // Verify Time Series returns points
        List<TimeSeriesPointDto> timeSeries = telemetryService.getTimeSeries(zone.getId(), SensorType.OCCUPANCY);
        assertNotNull(timeSeries);
        assertTrue(timeSeries.size() >= 2);
    }
}
