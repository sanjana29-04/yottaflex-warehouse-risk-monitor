package com.warehouse.riskmonitor.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "sensor_zone_mappings", indexes = {
    @Index(name = "idx_mapping_sensor", columnList = "sensor_id"),
    @Index(name = "idx_mapping_zone", columnList = "zone_id"),
    @Index(name = "idx_mapping_active", columnList = "is_active")
})
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class SensorZoneMapping {

    @Id
    @Column(name = "id", length = 64, nullable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sensor_id", nullable = false, foreignKey = @ForeignKey(name = "fk_mapping_sensor"))
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "zone"})
    private Sensor sensor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "zone_id", nullable = false, foreignKey = @ForeignKey(name = "fk_mapping_zone"))
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "warehouse", "sensors"})
    private Zone zone;

    @Column(name = "mapped_at", nullable = false)
    private LocalDateTime mappedAt;

    @Column(name = "unmapped_at")
    private LocalDateTime unmappedAt;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "rationale", length = 255)
    private String rationale;

    public SensorZoneMapping() {}

    public SensorZoneMapping(String id, Sensor sensor, Zone zone, LocalDateTime mappedAt,
                             LocalDateTime unmappedAt, Boolean isActive, String rationale) {
        this.id = id;
        this.sensor = sensor;
        this.zone = zone;
        this.mappedAt = mappedAt;
        this.unmappedAt = unmappedAt;
        this.isActive = isActive != null ? isActive : true;
        this.rationale = rationale;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public Sensor getSensor() { return sensor; }
    public void setSensor(Sensor sensor) { this.sensor = sensor; }

    public Zone getZone() { return zone; }
    public void setZone(Zone zone) { this.zone = zone; }

    public LocalDateTime getMappedAt() { return mappedAt; }
    public void setMappedAt(LocalDateTime mappedAt) { this.mappedAt = mappedAt; }

    public LocalDateTime getUnmappedAt() { return unmappedAt; }
    public void setUnmappedAt(LocalDateTime unmappedAt) { this.unmappedAt = unmappedAt; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public String getRationale() { return rationale; }
    public void setRationale(String rationale) { this.rationale = rationale; }
}

