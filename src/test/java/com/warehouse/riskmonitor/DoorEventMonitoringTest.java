package com.warehouse.riskmonitor;

import com.warehouse.riskmonitor.entity.Alert;
import com.warehouse.riskmonitor.entity.DoorEvent;
import com.warehouse.riskmonitor.entity.Warehouse;
import com.warehouse.riskmonitor.entity.Zone;
import com.warehouse.riskmonitor.entity.enums.*;
import com.warehouse.riskmonitor.repository.AlertRepository;
import com.warehouse.riskmonitor.service.DoorEventService;
import com.warehouse.riskmonitor.service.WarehouseService;
import com.warehouse.riskmonitor.service.ZoneService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class DoorEventMonitoringTest {

    @Autowired
    private WarehouseService warehouseService;

    @Autowired
    private ZoneService zoneService;

    @Autowired
    private DoorEventService doorEventService;

    @Autowired
    private AlertRepository alertRepository;

    @Test
    @DisplayName("Verify compliant door opening within SLA permitted duration")
    public void testCompliantDoorEvent() {
        Warehouse wh = new Warehouse("WH-DOOR-01", "Door Test Facility", "Chicago, IL", 25000, 2, WarehouseStatus.ACTIVE);
        warehouseService.saveWarehouse(wh);

        Zone zone = new Zone("ZONE-WH-DOOR-01-001", wh, "Freezer Vault", ZoneType.FROZEN, 200, -25.0, -18.0, 30.0, 60.0, 4);
        zoneService.saveZone(zone, wh.getId());

        LocalDateTime now = LocalDateTime.now();
        // Door open for 45 seconds (permitted: 90 seconds)
        DoorEvent event = doorEventService.recordDoorEvent(
                zone.getId(), "Dock Bay 1 Air-Lock", now.minusSeconds(45), now, 90);

        assertNotNull(event);
        assertNotNull(event.getId());
        assertEquals(DoorStatus.NORMAL, event.getStatus());
        assertEquals(45, event.getDurationSeconds());
        assertFalse(event.getAlertTriggered());

        Page<DoorEvent> normalEvents = doorEventService.getDoorEventsPaged(zone.getId(), DoorStatus.NORMAL, PageRequest.of(0, 10));
        assertTrue(normalEvents.getTotalElements() >= 1);
    }

    @Test
    @DisplayName("Verify prolonged door opening triggers EXCEEDED_DURATION status and alert creation")
    public void testProlongedDoorExceedsSlaAndTriggersAlert() {
        Warehouse wh = new Warehouse("WH-DOOR-02", "Cold Hub", "Dallas, TX", 30000, 2, WarehouseStatus.ACTIVE);
        warehouseService.saveWarehouse(wh);

        Zone zone = new Zone("ZONE-WH-DOOR-02-001", wh, "Cold Room 1", ZoneType.COLD_STORAGE, 300, 2.0, 8.0, 40.0, 70.0, 6);
        zoneService.saveZone(zone, wh.getId());

        LocalDateTime now = LocalDateTime.now();
        // Door open for 300 seconds (permitted: 60 seconds)
        DoorEvent event = doorEventService.recordDoorEvent(
                zone.getId(), "Rapid Roll Door East", now.minusSeconds(300), now, 60);

        assertNotNull(event);
        assertEquals(DoorStatus.EXCEEDED_DURATION, event.getStatus());
        assertEquals(300, event.getDurationSeconds());
        assertTrue(event.getAlertTriggered());

        // Verify alert was automatically created
        List<Alert> alerts = alertRepository.findByZone_Id(zone.getId());
        assertFalse(alerts.isEmpty(), "An alert must be generated for prolonged door opening");

        Alert doorAlert = alerts.stream()
                .filter(a -> a.getAlertType() == AlertType.DOOR_OPEN_PROLONGED)
                .findFirst()
                .orElse(null);

        assertNotNull(doorAlert, "DOOR_OPEN_PROLONGED alert must exist");
        assertEquals(RiskLevel.HIGH, doorAlert.getRiskLevel());
        assertEquals(AlertStatus.OPEN, doorAlert.getStatus());
        assertEquals(300.0, doorAlert.getTriggerValue());
        assertEquals(60.0, doorAlert.getThresholdValue());
        assertTrue(doorAlert.getMessage().contains("Rapid Roll Door East"));
    }

    @Test
    @DisplayName("Verify ongoing door opening with null closedAt")
    public void testOngoingDoorOpening() {
        Warehouse wh = new Warehouse("WH-DOOR-03", "Logistics Hub", "Atlanta, GA", 20000, 1, WarehouseStatus.ACTIVE);
        warehouseService.saveWarehouse(wh);

        Zone zone = new Zone("ZONE-WH-DOOR-03-001", wh, "Buffer Zone", ZoneType.BUFFER, 250, 10.0, 20.0, 40.0, 70.0, 5);
        zoneService.saveZone(zone, wh.getId());

        DoorEvent event = doorEventService.recordDoorEvent(
                zone.getId(), "Personnel Door 2", LocalDateTime.now(), null, 60);

        assertNotNull(event);
        assertNull(event.getClosedAt());
        assertNull(event.getDurationSeconds());
        assertEquals(DoorStatus.NORMAL, event.getStatus());
        assertFalse(event.getAlertTriggered());
    }
}
