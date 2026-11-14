package com.warehouse.riskmonitor.service;

import com.warehouse.riskmonitor.entity.Warehouse;
import com.warehouse.riskmonitor.entity.Zone;
import com.warehouse.riskmonitor.entity.enums.ZoneStatus;
import com.warehouse.riskmonitor.exception.ResourceNotFoundException;
import com.warehouse.riskmonitor.repository.WarehouseRepository;
import com.warehouse.riskmonitor.repository.ZoneRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ZoneService {

    private final ZoneRepository zoneRepository;
    private final WarehouseRepository warehouseRepository;
    private volatile List<Zone> cachedZones;
    private volatile long lastZonesFetch = 0;

    public ZoneService(ZoneRepository zoneRepository, WarehouseRepository warehouseRepository) {
        this.zoneRepository = zoneRepository;
        this.warehouseRepository = warehouseRepository;
    }

    public List<Zone> getAllZones() {
        if (cachedZones != null && (System.currentTimeMillis() - lastZonesFetch < 10000)) {
            return cachedZones;
        }
        cachedZones = zoneRepository.findAll();
        lastZonesFetch = System.currentTimeMillis();
        return cachedZones;
    }

    public List<Zone> getZonesByWarehouse(String warehouseId) {
        return zoneRepository.findByWarehouse_Id(warehouseId);
    }

    public Page<Zone> getZonesPaged(ZoneStatus status, String warehouseId, String search, Pageable pageable) {
        return zoneRepository.findFiltered(status, warehouseId, search, pageable);
    }

    public Zone getZoneById(String id) {
        return zoneRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Zone with id " + id + " not found"));
    }

    @Transactional
    public Zone saveZone(Zone zone, String warehouseId) {
        Warehouse wh = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse " + warehouseId + " not found"));
        zone.setWarehouse(wh);
        if (zone.getId() == null || zone.getId().trim().isEmpty()) {
            long count = zoneRepository.findByWarehouse_Id(warehouseId).size() + 1;
            zone.setId(String.format("ZONE-%s-%03d", warehouseId, count));
        }
        if (zone.getStatus() == null) {
            zone.setStatus(ZoneStatus.NORMAL);
        }
        cachedZones = null;
        return zoneRepository.save(zone);
    }

    @Transactional
    public Zone updateZone(String id, Zone updated) {
        Zone existing = getZoneById(id);
        existing.setName(updated.getName());
        existing.setZoneType(updated.getZoneType());
        existing.setCapacityLots(updated.getCapacityLots());
        existing.setTemperatureMin(updated.getTemperatureMin());
        existing.setTemperatureMax(updated.getTemperatureMax());
        existing.setHumidityMin(updated.getHumidityMin());
        existing.setHumidityMax(updated.getHumidityMax());
        existing.setMaxOccupancy(updated.getMaxOccupancy());
        if (updated.getStatus() != null) {
            existing.setStatus(updated.getStatus());
        }
        cachedZones = null;
        return zoneRepository.save(existing);
    }

    @Transactional
    public void deleteZone(String id) {
        if (!zoneRepository.existsById(id)) {
            throw new ResourceNotFoundException("Zone with id " + id + " not found");
        }
        zoneRepository.deleteById(id);
    }

    public long getCountByStatus(ZoneStatus status) {
        return zoneRepository.countByStatus(status);
    }
}
