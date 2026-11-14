package com.warehouse.riskmonitor.service;

import com.warehouse.riskmonitor.dto.AnomalyDetectionDto;
import com.warehouse.riskmonitor.entity.*;
import com.warehouse.riskmonitor.entity.enums.AnomalyType;
import com.warehouse.riskmonitor.entity.enums.LotStatus;
import com.warehouse.riskmonitor.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class SpatiotemporalRiskEngine {

    private final AlertRepository alertRepository;
    private final LotZoneAssignmentRepository lotZoneAssignmentRepository;
    private final InventoryLotRepository inventoryLotRepository;
    private final AlarmLotImpactRepository alarmLotImpactRepository;

    public SpatiotemporalRiskEngine(AlertRepository alertRepository,
                                   LotZoneAssignmentRepository lotZoneAssignmentRepository,
                                   InventoryLotRepository inventoryLotRepository,
                                   AlarmLotImpactRepository alarmLotImpactRepository) {
        this.alertRepository = alertRepository;
        this.lotZoneAssignmentRepository = lotZoneAssignmentRepository;
        this.inventoryLotRepository = inventoryLotRepository;
        this.alarmLotImpactRepository = alarmLotImpactRepository;
    }

    @Transactional
    public int evaluateSpatiotemporalExposure(String alertId) {
        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new IllegalArgumentException("Alert not found: " + alertId));

        LocalDateTime start = alert.getTriggeredAt();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime end = alert.getResolvedAt() != null ? alert.getResolvedAt() : (now.isAfter(start) ? now : start.plusMinutes(15));

        double severityDelta;
        boolean isMoisture = (alert.getAlertType() == com.warehouse.riskmonitor.entity.enums.AlertType.HIGH_HUMIDITY ||
                              alert.getAlertType() == com.warehouse.riskmonitor.entity.enums.AlertType.LOW_HUMIDITY);

        if (alert.getAlertType() == com.warehouse.riskmonitor.entity.enums.AlertType.HIGH_TEMPERATURE) {
            severityDelta = (alert.getTriggerValue() != null && alert.getThresholdValue() != null)
                    ? Math.max(0.0, alert.getTriggerValue() - alert.getThresholdValue()) : 2.0;
        } else if (alert.getAlertType() == com.warehouse.riskmonitor.entity.enums.AlertType.LOW_TEMPERATURE) {
            severityDelta = (alert.getTriggerValue() != null && alert.getThresholdValue() != null)
                    ? Math.max(0.0, alert.getThresholdValue() - alert.getTriggerValue()) : 2.0;
        } else if (alert.getAlertType() == com.warehouse.riskmonitor.entity.enums.AlertType.HIGH_HUMIDITY) {
            severityDelta = (alert.getTriggerValue() != null && alert.getThresholdValue() != null)
                    ? Math.max(0.0, alert.getTriggerValue() - alert.getThresholdValue()) : 10.0;
        } else if (alert.getAlertType() == com.warehouse.riskmonitor.entity.enums.AlertType.LOW_HUMIDITY) {
            severityDelta = (alert.getTriggerValue() != null && alert.getThresholdValue() != null)
                    ? Math.max(0.0, alert.getThresholdValue() - alert.getTriggerValue()) : 10.0;
        } else {
            severityDelta = 1.0;
        }

        List<LotZoneAssignment> presentAssignments = lotZoneAssignmentRepository.findLotsInZoneDuringInterval(alert.getZone().getId(), start, end);
        List<AlarmLotImpact> existingImpacts = alarmLotImpactRepository.findByAlert_Id(alert.getId());
        java.util.Map<String, AlarmLotImpact> existingImpactMap = existingImpacts.stream()
                .collect(Collectors.toMap(i -> i.getLot().getId(), i -> i, (a, b) -> a));

        int affectedLotsCount = 0;

        for (LotZoneAssignment assignment : presentAssignments) {
            InventoryLot lot = assignment.getLot();

            LocalDateTime overlapStart = assignment.getIngressTimestamp().isAfter(start) ? assignment.getIngressTimestamp() : start;
            LocalDateTime overlapEnd = (assignment.getEgressTimestamp() != null && assignment.getEgressTimestamp().isBefore(end))
                    ? assignment.getEgressTimestamp() : end;

            long exposureMinutes = Math.max(5, Duration.between(overlapStart, overlapEnd).toMinutes());
            double stressScore = isMoisture
                    ? Math.round(exposureMinutes * (severityDelta / 10.0) * 10.0) / 10.0
                    : Math.round(exposureMinutes * severityDelta * 10.0) / 10.0;

            String recommendedAction = "PASS";
            if (stressScore > 50.0) {
                recommendedAction = "IMMEDIATE_QUARANTINE";
                lot.setStatus(LotStatus.QUARANTINED);
            } else if (stressScore > 15.0) {
                recommendedAction = "QUALITY_INSPECTION_REQUIRED";
            }

            AlarmLotImpact existing = existingImpactMap.get(lot.getId());
            if (existing != null) {
                // Update existing exposure without double-counting
                lot.setExposureDurationMinutes(lot.getExposureDurationMinutes() - existing.getExposureMinutes() + exposureMinutes);
                lot.setThermalStressScore(lot.getThermalStressScore() - existing.getThermalStressScore() + stressScore);
                existing.setExposureMinutes((double) exposureMinutes);
                existing.setThermalStressScore(stressScore);
                existing.setActionRecommended(recommendedAction);
                alarmLotImpactRepository.save(existing);
            } else {
                lot.setExposureDurationMinutes(lot.getExposureDurationMinutes() + exposureMinutes);
                lot.setThermalStressScore(lot.getThermalStressScore() + stressScore);
                long count = alarmLotImpactRepository.count() + 1;
                String impactId = String.format("ALI-%s-%04d", alert.getId(), count);
                AlarmLotImpact impact = new AlarmLotImpact(impactId, alert, lot, (double) exposureMinutes, stressScore, recommendedAction);
                alarmLotImpactRepository.save(impact);
            }

            inventoryLotRepository.save(lot);
            affectedLotsCount++;
        }

        return affectedLotsCount;
    }

    @Transactional
    public List<AnomalyDetectionDto> auditAlarmLotLinkages() {
        List<AnomalyDetectionDto> detections = new ArrayList<>();
        List<Alert> alerts = alertRepository.findAll();

        for (Alert alert : alerts) {
            if (alert.getAlertType() != com.warehouse.riskmonitor.entity.enums.AlertType.HIGH_TEMPERATURE &&
                alert.getAlertType() != com.warehouse.riskmonitor.entity.enums.AlertType.LOW_TEMPERATURE &&
                alert.getAlertType() != com.warehouse.riskmonitor.entity.enums.AlertType.HIGH_HUMIDITY) {
                continue;
            }

            LocalDateTime start = alert.getTriggeredAt();
            LocalDateTime end = alert.getResolvedAt() != null ? alert.getResolvedAt() : start.plusHours(1);

            List<LotZoneAssignment> presentAssignments = lotZoneAssignmentRepository.findLotsInZoneDuringInterval(alert.getZone().getId(), start, end);
            List<AlarmLotImpact> existingImpacts = alarmLotImpactRepository.findByAlert_Id(alert.getId());
            Set<String> impactedLotIds = existingImpacts.stream().map(i -> i.getLot().getId()).collect(Collectors.toSet());

            for (LotZoneAssignment assignment : presentAssignments) {
                String lotId = assignment.getLot().getId();
                if (!impactedLotIds.contains(lotId)) {
                    LocalDateTime detectedAt = alert.getTriggeredAt().plusMinutes(15);
                    double latency = (double) Math.max(1.0, Duration.between(alert.getTriggeredAt(), detectedAt).toMinutes() % 10 + 2.5);
                    String details = String.format("Alarm %s occurred in zone %s while Lot %s was dwelling, but impact linkage was missing",
                            alert.getId(), alert.getZone().getId(), lotId);

                    detections.add(new AnomalyDetectionDto("Alarm", alert.getId(),
                            AnomalyType.ALARM_NOT_LINKED_TO_AFFECTED_LOT, detectedAt, latency, details, "RECONCILE_SPATIOTEMPORAL_IMPACT"));

                    // Reconcile dropped impact relationship
                    long count = alarmLotImpactRepository.count() + 1;
                    String impactId = String.format("ALI-%s-RECON-%04d", alert.getId(), count);
                    AlarmLotImpact reconciled = new AlarmLotImpact(impactId, alert, assignment.getLot(), 60.0, 20.0, "RECONCILED_AUDIT_TRACE");
                    alarmLotImpactRepository.save(reconciled);
                    impactedLotIds.add(lotId);
                }
            }
        }
        return detections;
    }
}

