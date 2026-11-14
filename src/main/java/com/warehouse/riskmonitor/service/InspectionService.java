package com.warehouse.riskmonitor.service;

import com.warehouse.riskmonitor.entity.Alert;
import com.warehouse.riskmonitor.entity.Inspection;
import com.warehouse.riskmonitor.entity.Zone;
import com.warehouse.riskmonitor.entity.enums.InspectionStatus;
import com.warehouse.riskmonitor.exception.ResourceNotFoundException;
import com.warehouse.riskmonitor.repository.AlertRepository;
import com.warehouse.riskmonitor.repository.InspectionRepository;
import com.warehouse.riskmonitor.repository.ZoneRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class InspectionService {

    private final InspectionRepository inspectionRepository;
    private final ZoneRepository zoneRepository;
    private final AlertRepository alertRepository;

    public InspectionService(InspectionRepository inspectionRepository,
                             ZoneRepository zoneRepository,
                             AlertRepository alertRepository) {
        this.inspectionRepository = inspectionRepository;
        this.zoneRepository = zoneRepository;
        this.alertRepository = alertRepository;
    }

    public Page<Inspection> getInspectionsPaged(String zoneId, InspectionStatus status, Pageable pageable) {
        return inspectionRepository.findFiltered(zoneId, status, pageable);
    }

    public List<Inspection> getAllInspections() {
        return inspectionRepository.findAll();
    }

    public Inspection getInspectionById(String id) {
        return inspectionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Inspection " + id + " not found"));
    }

    @Transactional
    public Inspection createInspection(String zoneId, String alertId, String inspectorName, String observations, Boolean actionRequired) {
        Zone zone = zoneRepository.findById(zoneId)
                .orElseThrow(() -> new ResourceNotFoundException("Zone " + zoneId + " not found"));
        Alert alert = alertId != null && !alertId.trim().isEmpty() ? alertRepository.findById(alertId).orElse(null) : null;

        long count = inspectionRepository.count() + 1;
        String id = String.format("INSP-2026-%06d", count);
        Inspection insp = new Inspection(id, zone, alert, inspectorName, LocalDateTime.now(),
                InspectionStatus.PENDING, observations, null, actionRequired);
        return inspectionRepository.save(insp);
    }

    @Transactional
    public Inspection completeInspection(String id, String findingsSummary) {
        Inspection insp = getInspectionById(id);
        insp.setStatus(InspectionStatus.COMPLETED);
        insp.setFindingsSummary(findingsSummary);
        return inspectionRepository.save(insp);
    }

    public long getCountByStatus(InspectionStatus status) {
        return inspectionRepository.countByStatus(status);
    }
}

