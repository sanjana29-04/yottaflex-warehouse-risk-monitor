package com.warehouse.riskmonitor.dto;

import com.warehouse.riskmonitor.entity.enums.AnomalyType;
import java.time.LocalDateTime;

public class AnomalyDetectionDto {

    private String entityType;
    private String entityId;
    private AnomalyType anomalyType;
    private LocalDateTime detectedAt;
    private double detectionLatencyMinutes;
    private String details;
    private String recommendedAction;

    public AnomalyDetectionDto() {}

    public AnomalyDetectionDto(String entityType, String entityId, AnomalyType anomalyType,
                               LocalDateTime detectedAt, double detectionLatencyMinutes,
                               String details, String recommendedAction) {
        this.entityType = entityType;
        this.entityId = entityId;
        this.anomalyType = anomalyType;
        this.detectedAt = detectedAt;
        this.detectionLatencyMinutes = detectionLatencyMinutes;
        this.details = details;
        this.recommendedAction = recommendedAction;
    }

    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }

    public String getEntityId() { return entityId; }
    public void setEntityId(String entityId) { this.entityId = entityId; }

    public AnomalyType getAnomalyType() { return anomalyType; }
    public void setAnomalyType(AnomalyType anomalyType) { this.anomalyType = anomalyType; }

    public LocalDateTime getDetectedAt() { return detectedAt; }
    public void setDetectedAt(LocalDateTime detectedAt) { this.detectedAt = detectedAt; }

    public double getDetectionLatencyMinutes() { return detectionLatencyMinutes; }
    public void setDetectionLatencyMinutes(double detectionLatencyMinutes) { this.detectionLatencyMinutes = detectionLatencyMinutes; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }

    public String getRecommendedAction() { return recommendedAction; }
    public void setRecommendedAction(String recommendedAction) { this.recommendedAction = recommendedAction; }
}

