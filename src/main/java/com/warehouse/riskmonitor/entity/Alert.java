package com.warehouse.riskmonitor.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.warehouse.riskmonitor.entity.enums.AlertStatus;
import com.warehouse.riskmonitor.entity.enums.AlertType;
import com.warehouse.riskmonitor.entity.enums.RiskLevel;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "alerts", indexes = {
    @Index(name = "idx_alert_zone", columnList = "zone_id"),
    @Index(name = "idx_alert_status", columnList = "status"),
    @Index(name = "idx_alert_risk", columnList = "risk_level"),
    @Index(name = "idx_alert_time", columnList = "triggered_at")
})
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Alert {

    @Id
    @Column(name = "id", length = 64, nullable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "zone_id", nullable = false, foreignKey = @ForeignKey(name = "fk_alert_zone"))
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "warehouse", "sensors"})
    private Zone zone;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sensor_id", foreignKey = @ForeignKey(name = "fk_alert_sensor"))
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "zone"})
    private Sensor sensor;

    @Enumerated(EnumType.STRING)
    @Column(name = "alert_type", length = 32, nullable = false)
    private AlertType alertType;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", length = 16, nullable = false)
    private RiskLevel riskLevel = RiskLevel.MEDIUM;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 32, nullable = false)
    private AlertStatus status = AlertStatus.OPEN;

    @Column(name = "triggered_at", nullable = false)
    private LocalDateTime triggeredAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "trigger_value")
    private Double triggerValue;

    @Column(name = "threshold_value")
    private Double thresholdValue;

    @Column(name = "message", length = 512, nullable = false)
    private String message;

    @Column(name = "acknowledged_by", length = 64)
    private String acknowledgedBy;

    @Column(name = "resolved_by", length = 64)
    private String resolvedBy;

    @Column(name = "notes", length = 1024)
    private String notes;

    public Alert() {}

    public Alert(String id, Zone zone, Sensor sensor, AlertType alertType, RiskLevel riskLevel,
                 AlertStatus status, LocalDateTime triggeredAt, Double triggerValue, Double thresholdValue,
                 String message) {
        this.id = id;
        this.zone = zone;
        this.sensor = sensor;
        this.alertType = alertType;
        this.riskLevel = riskLevel != null ? riskLevel : RiskLevel.MEDIUM;
        this.status = status != null ? status : AlertStatus.OPEN;
        this.triggeredAt = triggeredAt != null ? triggeredAt : LocalDateTime.now();
        this.triggerValue = triggerValue;
        this.thresholdValue = thresholdValue;
        this.message = message;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public Zone getZone() { return zone; }
    public void setZone(Zone zone) { this.zone = zone; }

    public String getZoneId() { return zone != null ? zone.getId() : null; }

    public Sensor getSensor() { return sensor; }
    public void setSensor(Sensor sensor) { this.sensor = sensor; }

    public String getSensorId() { return sensor != null ? sensor.getId() : null; }

    public AlertType getAlertType() { return alertType; }
    public void setAlertType(AlertType alertType) { this.alertType = alertType; }

    public RiskLevel getRiskLevel() { return riskLevel; }
    public void setRiskLevel(RiskLevel riskLevel) { this.riskLevel = riskLevel; }

    public AlertStatus getStatus() { return status; }
    public void setStatus(AlertStatus status) { this.status = status; }

    public LocalDateTime getTriggeredAt() { return triggeredAt; }
    public void setTriggeredAt(LocalDateTime triggeredAt) { this.triggeredAt = triggeredAt; }

    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(LocalDateTime resolvedAt) { this.resolvedAt = resolvedAt; }

    public Double getTriggerValue() { return triggerValue; }
    public void setTriggerValue(Double triggerValue) { this.triggerValue = triggerValue; }

    public Double getThresholdValue() { return thresholdValue; }
    public void setThresholdValue(Double thresholdValue) { this.thresholdValue = thresholdValue; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getAcknowledgedBy() { return acknowledgedBy; }
    public void setAcknowledgedBy(String acknowledgedBy) { this.acknowledgedBy = acknowledgedBy; }

    public String getResolvedBy() { return resolvedBy; }
    public void setResolvedBy(String resolvedBy) { this.resolvedBy = resolvedBy; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
