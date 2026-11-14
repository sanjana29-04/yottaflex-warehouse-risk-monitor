package com.warehouse.riskmonitor;

import com.warehouse.riskmonitor.entity.Warehouse;
import com.warehouse.riskmonitor.entity.Zone;
import com.warehouse.riskmonitor.entity.enums.WarehouseStatus;
import com.warehouse.riskmonitor.entity.enums.ZoneStatus;
import com.warehouse.riskmonitor.entity.enums.ZoneType;
import com.warehouse.riskmonitor.service.WarehouseService;
import com.warehouse.riskmonitor.service.ZoneService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class ZoneServiceTest {

    @Autowired
    private WarehouseService warehouseService;

    @Autowired
    private ZoneService zoneService;

    @Test
    public void testZoneCreationAndQuery() {
        Warehouse wh = new Warehouse("WH-TEST-02", "Central Depot", "Chicago, IL", 20000, 3, WarehouseStatus.ACTIVE);
        warehouseService.saveWarehouse(wh);

        Zone zone = new Zone("ZONE-WH-TEST-02-001", wh, "Vaccine Cold Vault", ZoneType.FROZEN, 400, -25.0, -18.0, 30.0, 60.0, 8);
        Zone saved = zoneService.saveZone(zone, wh.getId());

        assertNotNull(saved);
        assertEquals("ZONE-WH-TEST-02-001", saved.getId());
        assertEquals(ZoneType.FROZEN, saved.getZoneType());
        assertEquals(ZoneStatus.NORMAL, saved.getStatus());

        Zone retrieved = zoneService.getZoneById("ZONE-WH-TEST-02-001");
        assertEquals("Vaccine Cold Vault", retrieved.getName());

        retrieved.setStatus(ZoneStatus.WARNING);
        Zone updated = zoneService.updateZone(retrieved.getId(), retrieved);
        assertEquals(ZoneStatus.WARNING, updated.getStatus());
    }
}

