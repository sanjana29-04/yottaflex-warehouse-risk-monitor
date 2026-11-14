package com.warehouse.riskmonitor;

import com.warehouse.riskmonitor.entity.Warehouse;
import com.warehouse.riskmonitor.entity.enums.WarehouseStatus;
import com.warehouse.riskmonitor.service.WarehouseService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class WarehouseServiceTest {

    @Autowired
    private WarehouseService warehouseService;

    @Test
    public void testWarehouseCrud() {
        Warehouse wh = new Warehouse("WH-TEST-01", "Test Logistic Center", "Dallas, TX", 30000, 5, WarehouseStatus.ACTIVE);
        Warehouse saved = warehouseService.saveWarehouse(wh);

        assertNotNull(saved);
        assertEquals("WH-TEST-01", saved.getId());
        assertEquals("Test Logistic Center", saved.getName());

        Warehouse retrieved = warehouseService.getWarehouseById("WH-TEST-01");
        assertNotNull(retrieved);
        assertEquals("Dallas, TX", retrieved.getLocation());

        retrieved.setLocation("Austin, TX");
        Warehouse updated = warehouseService.updateWarehouse("WH-TEST-01", retrieved);
        assertEquals("Austin, TX", updated.getLocation());

        warehouseService.deleteWarehouse("WH-TEST-01");
        assertThrows(RuntimeException.class, () -> warehouseService.getWarehouseById("WH-TEST-01"));
    }
}

