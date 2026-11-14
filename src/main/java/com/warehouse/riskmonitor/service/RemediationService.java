package com.warehouse.riskmonitor.service;

import com.warehouse.riskmonitor.entity.Alert;
import com.warehouse.riskmonitor.entity.Inspection;
import com.warehouse.riskmonitor.entity.InventoryLot;
import com.warehouse.riskmonitor.entity.Remediation;
import com.warehouse.riskmonitor.entity.enums.RemediationStatus;
import com.warehouse.riskmonitor.entity.enums.RemediationType;
import com.warehouse.riskmonitor.exception.ResourceNotFoundException;
import com.warehouse.riskmonitor.repository.AlertRepository;
import com.warehouse.riskmonitor.repository.InspectionRepository;
import com.warehouse.riskmonitor.repository.InventoryLotRepository;
import com.warehouse.riskmonitor.repository.RemediationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class RemediationService {

    private final RemediationRepository remediationRepository;
    private final AlertRepository alertRepository;
    private final InspectionRepository inspectionRepository;
    private final InventoryLotRepository inventoryLotRepository;

    public RemediationService(RemediationRepository remediationRepository,
                              AlertRepository alertRepository,
                              InspectionRepository inspectionRepository,
                              InventoryLotRepository inventoryLotRepository) {
        this.remediationRepository = remediationRepository;
        this.alertRepository = alertRepository;
        this.inspectionRepository = inspectionRepository;
        this.inventoryLotRepository = inventoryLotRepository;
    }

    public Page<Remediation> getRemediationsPaged(RemediationStatus status, RemediationType type, Pageable pageable) {
        return remediationRepository.findFiltered(status, type, pageable);
    }

    public List<Remediation> getAllRemediations() {
        return remediationRepository.findAll();
    }

    public Remediation getRemediationById(String id) {
        return remediationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Remediation " + id + " not found"));
    }

    @Transactional
    public Remediation createRemediation(String alertId, String inspectionId, String lotId,
                                        String title, RemediationType type, String assignedTo, Double cost) {
        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new ResourceNotFoundException("Alert " + alertId + " not found"));
        Inspection insp = inspectionId != null && !inspectionId.trim().isEmpty() ? inspectionRepository.findById(inspectionId).orElse(null) : null;
        InventoryLot lot = lotId != null && !lotId.trim().isEmpty() ? inventoryLotRepository.findById(lotId).orElse(null) : null;

        long count = remediationRepository.count() + 1;
        String id = String.format("REM-2026-%06d", count);
        Remediation rem = new Remediation(id, alert, insp, lot, title, type, assignedTo,
                RemediationStatus.SCHEDULED, cost, LocalDateTime.now(), null, null);
        return remediationRepository.save(rem);
    }

    @Transactional
    public Remediation completeRemediation(String id, String notes) {
        Remediation rem = getRemediationById(id);
        rem.setStatus(RemediationStatus.COMPLETED);
        rem.setCompletedAt(LocalDateTime.now());
        rem.setResolutionNotes(notes);
        return remediationRepository.save(rem);
    }

    public double getTotalCostImpact() {
        Double sum = remediationRepository.calculateTotalCostImpact();
        return sum != null ? sum : 0.0;
    }

    public long getCountByStatus(RemediationStatus status) {
        return remediationRepository.countByStatus(status);
    }
}

