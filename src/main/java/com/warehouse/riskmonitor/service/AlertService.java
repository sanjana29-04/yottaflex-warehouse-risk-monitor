package com.warehouse.riskmonitor.service;

import com.warehouse.riskmonitor.entity.Alert;
import com.warehouse.riskmonitor.entity.Zone;
import com.warehouse.riskmonitor.entity.enums.AlertStatus;
import com.warehouse.riskmonitor.entity.enums.AlertType;
import com.warehouse.riskmonitor.entity.enums.RiskLevel;
import com.warehouse.riskmonitor.exception.ResourceNotFoundException;
import com.warehouse.riskmonitor.repository.AlertRepository;
import com.warehouse.riskmonitor.repository.ZoneRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AlertService {

    private final AlertRepository alertRepository;
    private final ZoneRepository zoneRepository;
    private final SpatiotemporalRiskEngine spatiotemporalRiskEngine;

    public AlertService(AlertRepository alertRepository,
                        ZoneRepository zoneRepository,
                        SpatiotemporalRiskEngine spatiotemporalRiskEngine) {
        this.alertRepository = alertRepository;
        this.zoneRepository = zoneRepository;
        this.spatiotemporalRiskEngine = spatiotemporalRiskEngine;
    }

    public Page<Alert> getAlertsPaged(String zoneId, AlertStatus status, RiskLevel risk, AlertType type, Pageable pageable) {
        return alertRepository.findFiltered(zoneId, status, risk, type, pageable);
    }

    public List<Alert> getRecentAlerts(int limit) {
        return alertRepository.findFiltered(null, null, null, null, PageRequest.of(0, limit)).getContent();
    }

    public Alert getAlertById(String id) {
        return alertRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Alert " + id + " not found"));
    }

    @Transactional
    public Alert createAlert(String zoneId, AlertType type, RiskLevel risk, Double triggerVal, Double thresholdVal, String message) {
        Zone zone = zoneRepository.findById(zoneId)
                .orElseThrow(() -> new ResourceNotFoundException("Zone " + zoneId + " not found"));

        long count = alertRepository.count() + 1;
        String id = String.format("ALM-%s-%04d", zoneId, count);
        Alert alert = new Alert(id, zone, null, type, risk, AlertStatus.OPEN, LocalDateTime.now(), triggerVal, thresholdVal, message);
        Alert saved = alertRepository.save(alert);
        spatiotemporalRiskEngine.evaluateSpatiotemporalExposure(saved.getId());
        return saved;
    }

    @Transactional
    public Alert acknowledgeAlert(String alertId, String user, String notes) {
        Alert alert = getAlertById(alertId);
        alert.setStatus(AlertStatus.ACKNOWLEDGED);
        alert.setAcknowledgedBy(user != null ? user : "Operator");
        if (notes != null && !notes.trim().isEmpty()) {
            alert.setNotes((alert.getNotes() != null ? alert.getNotes() + "\n" : "") + "[ACK] " + notes);
        }
        return alertRepository.save(alert);
    }

    @Transactional
    public Alert resolveAlert(String alertId, String user, String notes) {
        Alert alert = getAlertById(alertId);
        alert.setStatus(AlertStatus.RESOLVED);
        alert.setResolvedAt(LocalDateTime.now());
        alert.setResolvedBy(user != null ? user : "Quality Engineer");
        if (notes != null && !notes.trim().isEmpty()) {
            alert.setNotes((alert.getNotes() != null ? alert.getNotes() + "\n" : "") + "[RESOLVED] " + notes);
        }
        return alertRepository.save(alert);
    }

    public long getCountByStatus(AlertStatus status) {
        return alertRepository.countByStatus(status);
    }

    public long getCountByRiskLevel(RiskLevel risk) {
        return alertRepository.countByRiskLevel(risk);
    }

    public long getCriticalAlertsCount() {
        return alertRepository.countByStatusAndRiskLevel(AlertStatus.OPEN, RiskLevel.CRITICAL);
    }

    public Map<String, Long> getRiskDistribution() {
        Map<String, Long> map = new HashMap<>();
        for (Object[] row : alertRepository.countByRiskLevelGroup()) {
            RiskLevel risk = (RiskLevel) row[0];
            Long count = (Long) row[1];
            map.put(risk.name(), count);
        }
        return map;
    }
}

