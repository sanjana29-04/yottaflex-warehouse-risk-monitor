package com.warehouse.riskmonitor.service;

import com.warehouse.riskmonitor.entity.InventoryLot;
import com.warehouse.riskmonitor.entity.LotZoneAssignment;
import com.warehouse.riskmonitor.entity.StorageRequirement;
import com.warehouse.riskmonitor.entity.Zone;
import com.warehouse.riskmonitor.entity.enums.LotStatus;
import com.warehouse.riskmonitor.entity.enums.ProductCategory;
import com.warehouse.riskmonitor.exception.ResourceNotFoundException;
import com.warehouse.riskmonitor.repository.InventoryLotRepository;
import com.warehouse.riskmonitor.repository.LotZoneAssignmentRepository;
import com.warehouse.riskmonitor.repository.StorageRequirementRepository;
import com.warehouse.riskmonitor.repository.ZoneRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class InventoryLotService {

    private final InventoryLotRepository inventoryLotRepository;
    private final ZoneRepository zoneRepository;
    private final StorageRequirementRepository storageRequirementRepository;
    private final LotZoneAssignmentRepository lotZoneAssignmentRepository;

    public InventoryLotService(InventoryLotRepository inventoryLotRepository,
                               ZoneRepository zoneRepository,
                               StorageRequirementRepository storageRequirementRepository,
                               LotZoneAssignmentRepository lotZoneAssignmentRepository) {
        this.inventoryLotRepository = inventoryLotRepository;
        this.zoneRepository = zoneRepository;
        this.storageRequirementRepository = storageRequirementRepository;
        this.lotZoneAssignmentRepository = lotZoneAssignmentRepository;
    }

    public Page<InventoryLot> getLotsPaged(String zoneId, LotStatus status, ProductCategory category, String search, Pageable pageable) {
        return inventoryLotRepository.findFiltered(zoneId, status, category, search, pageable);
    }

    public List<InventoryLot> getAllLots() {
        return inventoryLotRepository.findAll();
    }

    public InventoryLot getLotById(String id) {
        return inventoryLotRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory lot " + id + " not found"));
    }

    @Transactional
    public InventoryLot saveLot(InventoryLot lot, String requirementId, String zoneId) {
        StorageRequirement req = storageRequirementRepository.findById(requirementId)
                .orElseThrow(() -> new ResourceNotFoundException("Storage requirement " + requirementId + " not found"));
        Zone zone = zoneRepository.findById(zoneId)
                .orElseThrow(() -> new ResourceNotFoundException("Zone " + zoneId + " not found"));

        lot.setStorageRequirement(req);
        lot.setCurrentZone(zone);

        if (lot.getId() == null || lot.getId().trim().isEmpty()) {
            long count = inventoryLotRepository.count() + 1;
            lot.setId(String.format("LOT-2026-%06d", count));
        }
        if (lot.getIngressTimestamp() == null) {
            lot.setIngressTimestamp(LocalDateTime.now());
        }
        if (lot.getExpiryDate() == null) {
            lot.setExpiryDate(lot.getIngressTimestamp().plusMonths(12));
        }
        if (lot.getStatus() == null) {
            lot.setStatus(LotStatus.STORED);
        }

        InventoryLot saved = inventoryLotRepository.save(lot);

        // Record zone assignment
        String lzaId = String.format("LZA-%s-01", saved.getId());
        LotZoneAssignment lza = new LotZoneAssignment(lzaId, saved, zone, saved.getIngressTimestamp(), null);
        lotZoneAssignmentRepository.save(lza);

        return saved;
    }

    @Transactional
    public InventoryLot updateLotStatus(String lotId, LotStatus newStatus) {
        InventoryLot lot = getLotById(lotId);
        lot.setStatus(newStatus);
        return inventoryLotRepository.save(lot);
    }

    @Transactional
    public void deleteLot(String id) {
        if (!inventoryLotRepository.existsById(id)) {
            throw new ResourceNotFoundException("Lot " + id + " not found");
        }
        inventoryLotRepository.deleteById(id);
    }

    public long getTotalLotsCount() {
        return inventoryLotRepository.count();
    }

    public long getCompromisedLotsCount() {
        return inventoryLotRepository.countCompromisedLots();
    }

    public List<com.warehouse.riskmonitor.dto.AnomalyDetectionDto> detectIncompatibleStorageAllocations() {
        List<com.warehouse.riskmonitor.dto.AnomalyDetectionDto> detections = new java.util.ArrayList<>();
        List<InventoryLot> lots = inventoryLotRepository.findAll();

        for (InventoryLot lot : lots) {
            StorageRequirement req = lot.getStorageRequirement();
            Zone zone = lot.getCurrentZone();
            if (req == null || zone == null) continue;

            boolean tempIncompatible = zone.getTemperatureMin() > req.getTempMax() || zone.getTemperatureMax() < req.getTempMin();
            boolean humidityIncompatible = zone.getHumidityMin() > req.getHumidityMax() || zone.getHumidityMax() < req.getHumidityMin();

            if (tempIncompatible || humidityIncompatible) {
                LocalDateTime detectedAt = lot.getIngressTimestamp() != null ? lot.getIngressTimestamp().plusMinutes(5) : LocalDateTime.now();
                double latency = lot.getIngressTimestamp() != null
                        ? (double) Math.max(1.0, java.time.Duration.between(lot.getIngressTimestamp(), detectedAt).toMinutes()) : 3.0;
                String details = String.format("Lot %s requiring %s (%.1f°C to %.1f°C) is assigned to incompatible Zone %s (%.1f°C to %.1f°C)",
                        lot.getId(), req.getName(), req.getTempMin(), req.getTempMax(),
                        zone.getId(), zone.getTemperatureMin(), zone.getTemperatureMax());

                detections.add(new com.warehouse.riskmonitor.dto.AnomalyDetectionDto(
                        "InventoryLot", lot.getId(),
                        com.warehouse.riskmonitor.entity.enums.AnomalyType.LOT_ASSIGNED_INCOMPATIBLE_ZONE,
                        detectedAt, latency, details, "QUARANTINE_AND_REALLOCATE"));
            }
        }
        return detections;
    }
}

