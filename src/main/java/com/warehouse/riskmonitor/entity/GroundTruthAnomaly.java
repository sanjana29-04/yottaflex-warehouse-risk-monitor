package com.warehouse.riskmonitor.entity;

import com.warehouse.riskmonitor.entity.enums.AnomalyType;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ground_truth_anomalies", indexes = {
    @Index(name = "idx_gta_type", columnList = "anomaly_type"),
    @Index(name = "idx_gta_entity", columnList = "entity_type, entity_id")
})
public class GroundTruthAnomaly {

    @Id
    @Column(name = "id", length = 64, nullable = false)
    private String id;

    @Column(name = "entity_type", length = 32, nullable = false)
    private String entityType;

    @Column(name = "entity_id", length = 64, nullable = false)
    private String entityId;

    @Enumerated(EnumType.STRING)
    @Column(name = "anomaly_type", length = 48, nullable = false)
    private AnomalyType anomalyType;

    @Column(name = "injected_at", nullable = false)
    private LocalDateTime injectedAt = LocalDateTime.now();

    @Column(name = "description", length = 512, nullable = false)
    private String description;

    @Column(name = "expected_action", length = 256, nullable = false)
    private String expectedAction;

    @Column(name = "detected", nullable = false)
    private Boolean detected = false;

    @Column(name = "detection_latency_minutes")
    private Double detectionLatencyMinutes;

    public GroundTruthAnomaly() {}

    public GroundTruthAnomaly(String id, String entityType, String entityId, AnomalyType anomalyType,
                              LocalDateTime injectedAt, String description, String expectedAction,
                              Boolean detected, Double detectionLatencyMinutes) {
        this.id = id;
        this.entityType = entityType;
        this.entityId = entityId;
        this.anomalyType = anomalyType;
        this.injectedAt = injectedAt != null ? injectedAt : LocalDateTime.now();
        this.description = description;
        this.expectedAction = expectedAction;
        this.detected = detected != null ? detected : false;
        this.detectionLatencyMinutes = detectionLatencyMinutes;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }

    public String getEntityId() { return entityId; }
    public void setEntityId(String entityId) { this.entityId = entityId; }

    public AnomalyType getAnomalyType() { return anomalyType; }
    public void setAnomalyType(AnomalyType anomalyType) { this.anomalyType = anomalyType; }

    public LocalDateTime getInjectedAt() { return injectedAt; }
    public void setInjectedAt(LocalDateTime injectedAt) { this.injectedAt = injectedAt; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getExpectedAction() { return expectedAction; }
    public void setExpectedAction(String expectedAction) { this.expectedAction = expectedAction; }

    public Boolean getDetected() { return detected; }
    public void setDetected(Boolean detected) { this.detected = detected; }

    public Double getDetectionLatencyMinutes() { return detectionLatencyMinutes; }
    public void setDetectionLatencyMinutes(Double detectionLatencyMinutes) { this.detectionLatencyMinutes = detectionLatencyMinutes; }
}

