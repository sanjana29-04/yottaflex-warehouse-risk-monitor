package com.warehouse.riskmonitor.service;

import com.warehouse.riskmonitor.entity.Warehouse;
import com.warehouse.riskmonitor.entity.enums.WarehouseStatus;
import com.warehouse.riskmonitor.exception.ResourceNotFoundException;
import com.warehouse.riskmonitor.repository.WarehouseRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class WarehouseService {

    private final WarehouseRepository warehouseRepository;
    private volatile List<Warehouse> cachedWarehouses;
    private volatile long lastWarehousesFetch = 0;

    public WarehouseService(WarehouseRepository warehouseRepository) {
        this.warehouseRepository = warehouseRepository;
    }

    public List<Warehouse> getAllWarehouses() {
        if (cachedWarehouses != null && (System.currentTimeMillis() - lastWarehousesFetch < 10000)) {
            return cachedWarehouses;
        }
        cachedWarehouses = warehouseRepository.findAll();
        lastWarehousesFetch = System.currentTimeMillis();
        return cachedWarehouses;
    }

    public Page<Warehouse> getWarehousesPaged(String search, Pageable pageable) {
        if (search != null && !search.trim().isEmpty()) {
            return warehouseRepository.findByNameContainingIgnoreCaseOrLocationContainingIgnoreCase(search, search, pageable);
        }
        return warehouseRepository.findAll(pageable);
    }

    public Warehouse getWarehouseById(String id) {
        return warehouseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse with id " + id + " not found"));
    }

    @Transactional
    public Warehouse saveWarehouse(Warehouse warehouse) {
        if (warehouse.getId() == null || warehouse.getId().trim().isEmpty()) {
            long count = warehouseRepository.count() + 1;
            warehouse.setId(String.format("WH-%03d", count));
        }
        return warehouseRepository.save(warehouse);
    }

    @Transactional
    public Warehouse updateWarehouse(String id, Warehouse updated) {
        Warehouse existing = getWarehouseById(id);
        existing.setName(updated.getName());
        existing.setLocation(updated.getLocation());
        existing.setCapacitySqm(updated.getCapacitySqm());
        existing.setTotalZones(updated.getTotalZones());
        existing.setStatus(updated.getStatus() != null ? updated.getStatus() : WarehouseStatus.ACTIVE);
        return warehouseRepository.save(existing);
    }

    @Transactional
    public void deleteWarehouse(String id) {
        if (!warehouseRepository.existsById(id)) {
            throw new ResourceNotFoundException("Warehouse with id " + id + " not found");
        }
        warehouseRepository.deleteById(id);
    }

    public long getTotalWarehouseCount() {
        return warehouseRepository.count();
    }
}

