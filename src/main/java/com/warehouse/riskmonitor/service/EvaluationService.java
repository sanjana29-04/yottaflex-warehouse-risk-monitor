package com.warehouse.riskmonitor.service;

import com.warehouse.riskmonitor.dto.AnomalyDetectionDto;
import com.warehouse.riskmonitor.dto.EvaluationMetricsDto;
import com.warehouse.riskmonitor.entity.GroundTruthAnomaly;
import com.warehouse.riskmonitor.repository.GroundTruthAnomalyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class EvaluationService {

    private final GroundTruthAnomalyRepository anomalyRepository;
    private final InventoryLotService inventoryLotService;
    private final SensorService sensorService;
    private final TelemetryService telemetryService;
    private final SpatiotemporalRiskEngine spatiotemporalRiskEngine;

    public EvaluationService(GroundTruthAnomalyRepository anomalyRepository,
                             InventoryLotService inventoryLotService,
                             SensorService sensorService,
                             TelemetryService telemetryService,
                             SpatiotemporalRiskEngine spatiotemporalRiskEngine) {
        this.anomalyRepository = anomalyRepository;
        this.inventoryLotService = inventoryLotService;
        this.sensorService = sensorService;
        this.telemetryService = telemetryService;
        this.spatiotemporalRiskEngine = spatiotemporalRiskEngine;
    }

    @Transactional
    public EvaluationMetricsDto evaluateAgainstGroundTruth() {
        EvaluationMetricsDto dto = new EvaluationMetricsDto();
        List<GroundTruthAnomaly> anomalies = anomalyRepository.findAll();

        // 1. Run actual detection engines against the database records
        List<AnomalyDetectionDto> lotDetections = inventoryLotService.detectIncompatibleStorageAllocations();
        List<AnomalyDetectionDto> sensorDetections = sensorService.detectSensorTopologyConflicts();
        List<AnomalyDetectionDto> gapDetections = telemetryService.detectTelemetryGaps();
        List<AnomalyDetectionDto> alarmDetections = spatiotemporalRiskEngine.auditAlarmLotLinkages();

        List<AnomalyDetectionDto> allDetections = new ArrayList<>();
        allDetections.addAll(lotDetections);
        allDetections.addAll(sensorDetections);
        allDetections.addAll(gapDetections);
        allDetections.addAll(alarmDetections);

        // 2. Evaluate ground truth anomalies (True Positives vs False Negatives)
        int tp = 0;
        int fn = 0;
        double sumLatency = 0.0;
        int latencyCount = 0;

        Set<String> matchedDetectionKeys = new HashSet<>();

        for (GroundTruthAnomaly gta : anomalies) {
            Optional<AnomalyDetectionDto> matched = allDetections.stream()
                    .filter(d -> d.getAnomalyType() == gta.getAnomalyType() &&
                                 d.getEntityId().equalsIgnoreCase(gta.getEntityId()))
                    .findFirst();

            if (matched.isPresent()) {
                AnomalyDetectionDto det = matched.get();
                gta.setDetected(true);
                gta.setDetectionLatencyMinutes(det.getDetectionLatencyMinutes());
                tp++;
                sumLatency += det.getDetectionLatencyMinutes();
                latencyCount++;
                matchedDetectionKeys.add(det.getAnomalyType().name() + ":" + det.getEntityId());
            } else {
                gta.setDetected(false);
                fn++;
            }
            anomalyRepository.save(gta);
        }

        // 3. Evaluate False Positives (Detections that do not match ground truth)
        int fp = 0;
        for (AnomalyDetectionDto det : allDetections) {
            String key = det.getAnomalyType().name() + ":" + det.getEntityId();
            if (!matchedDetectionKeys.contains(key)) {
                boolean inGroundTruth = anomalies.stream()
                        .anyMatch(g -> g.getAnomalyType() == det.getAnomalyType() &&
                                       g.getEntityId().equalsIgnoreCase(det.getEntityId()));
                if (!inGroundTruth) {
                    fp++;
                }
            }
        }

        dto.setTruePositives(tp);
        dto.setFalsePositives(fp);
        dto.setFalseNegatives(fn);

        double precision = (tp + fp) > 0 ? (double) tp / (tp + fp) : 0.0;
        double recall = (tp + fn) > 0 ? (double) tp / (tp + fn) : 0.0;
        double f1 = (precision + recall) > 0 ? 2.0 * (precision * recall) / (precision + recall) : 0.0;
        double meanLatency = latencyCount > 0 ? (sumLatency / latencyCount) : 0.0;

        dto.setPrecision(Math.round(precision * 1000.0) / 1000.0);
        dto.setRecall(Math.round(recall * 1000.0) / 1000.0);
        dto.setF1Score(Math.round(f1 * 1000.0) / 1000.0);
        dto.setMeanDetectionLatencyMinutes(Math.round(meanLatency * 10.0) / 10.0);
        dto.setAnomalies(anomalies);

        return dto;
    }
}
