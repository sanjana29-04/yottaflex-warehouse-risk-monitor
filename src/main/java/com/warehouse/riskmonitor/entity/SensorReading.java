package com.warehouse.riskmonitor.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.warehouse.riskmonitor.entity.enums.ReadingStatus;
import com.warehouse.riskmonitor.entity.enums.SensorType;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "sensor_readings", indexes = {
    @Index(name = "idx_reading_sensor_time", columnList = "sensor_id, timestamp"),
    @Index(name = "idx_reading_zone_time", columnList = "zone_id, timestamp"),
    @Index(name = "idx_reading_type", columnList = "reading_type"),
    @Index(name = "idx_reading_status", columnList = "status"),
    @Index(name = "idx_reading_time", columnList = "timestamp")
})
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class SensorReading {

    @Id
    @Column(name = "id", length = 80, nullable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sensor_id", nullable = false, foreignKey = @ForeignKey(name = "fk_reading_sensor"))
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "zone"})
    private Sensor sensor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "zone_id", nullable = false, foreignKey = @ForeignKey(name = "fk_reading_zone"))
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "warehouse", "sensors"})
    private Zone zone;

    @Enumerated(EnumType.STRING)
    @Column(name = "reading_type", length = 32, nullable = false)
    private SensorType readingType;

    @Column(name = "reading_value", nullable = false)
    private Double value;

    @Column(name = "unit", length = 16, nullable = false)
    private String unit;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 32, nullable = false)
    private ReadingStatus status = ReadingStatus.NORMAL;

    @Column(name = "is_anomaly", nullable = false)
    private Boolean isAnomaly = false;

    public SensorReading() {}

    public SensorReading(String id, Sensor sensor, Zone zone, SensorType readingType, Double value,
                         String unit, LocalDateTime timestamp, ReadingStatus status, Boolean isAnomaly) {
        this.id = id;
        this.sensor = sensor;
        this.zone = zone;
        this.readingType = readingType;
        this.value = value;
        this.unit = unit;
        this.timestamp = timestamp;
        this.status = status != null ? status : ReadingStatus.NORMAL;
        this.isAnomaly = isAnomaly != null ? isAnomaly : false;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public Sensor getSensor() { return sensor; }
    public void setSensor(Sensor sensor) { this.sensor = sensor; }

    public String getSensorId() {
        return sensor != null ? sensor.getId() : null;
    }

    public Zone getZone() { return zone; }
    public void setZone(Zone zone) { this.zone = zone; }

    public String getZoneId() {
        return zone != null ? zone.getId() : null;
    }

    public SensorType getReadingType() { return readingType; }
    public void setReadingType(SensorType readingType) { this.readingType = readingType; }

    public Double getValue() { return value; }
    public void setValue(Double value) { this.value = value; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public ReadingStatus getStatus() { return status; }
    public void setStatus(ReadingStatus status) { this.status = status; }

    public Boolean getIsAnomaly() { return isAnomaly; }
    public void setIsAnomaly(Boolean isAnomaly) { this.isAnomaly = isAnomaly; }
}
