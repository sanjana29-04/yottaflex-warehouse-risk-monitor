package com.warehouse.riskmonitor.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.warehouse.riskmonitor.entity.enums.SensorStatus;
import com.warehouse.riskmonitor.entity.enums.SensorType;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "sensors", indexes = {
    @Index(name = "idx_sensor_zone", columnList = "zone_id"),
    @Index(name = "idx_sensor_type", columnList = "sensor_type"),
    @Index(name = "idx_sensor_status", columnList = "status")
})
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Sensor {

    @Id
    @Column(name = "id", length = 64, nullable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "zone_id", nullable = false, foreignKey = @ForeignKey(name = "fk_sensor_zone"))
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "warehouse", "sensors"})
    private Zone zone;

    @Enumerated(EnumType.STRING)
    @Column(name = "sensor_type", length = 32, nullable = false)
    private SensorType sensorType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 32, nullable = false)
    private SensorStatus status = SensorStatus.ACTIVE;

    @Column(name = "model_number", length = 64)
    private String modelNumber;

    @Column(name = "installation_date", nullable = false)
    private LocalDateTime installationDate = LocalDateTime.now();

    @Column(name = "last_calibration_date")
    private LocalDateTime lastCalibrationDate;

    @Column(name = "calibration_offset", nullable = false)
    private Double calibrationOffset = 0.0;

    @Column(name = "latest_reading_value")
    private Double latestReadingValue;

    @Column(name = "latest_reading_time")
    private LocalDateTime latestReadingTime;

    public Sensor() {}

    public Sensor(String id, Zone zone, SensorType sensorType, SensorStatus status, String modelNumber,
                  Double calibrationOffset) {
        this.id = id;
        this.zone = zone;
        this.sensorType = sensorType;
        this.status = status != null ? status : SensorStatus.ACTIVE;
        this.modelNumber = modelNumber;
        this.calibrationOffset = calibrationOffset != null ? calibrationOffset : 0.0;
        this.installationDate = LocalDateTime.now().minusDays(90);
        this.lastCalibrationDate = LocalDateTime.now().minusDays(15);
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public Zone getZone() { return zone; }
    public void setZone(Zone zone) { this.zone = zone; }

    public String getZoneId() {
        return zone != null ? zone.getId() : null;
    }

    public SensorType getSensorType() { return sensorType; }
    public void setSensorType(SensorType sensorType) { this.sensorType = sensorType; }

    public SensorStatus getStatus() { return status; }
    public void setStatus(SensorStatus status) { this.status = status; }

    public String getModelNumber() { return modelNumber; }
    public void setModelNumber(String modelNumber) { this.modelNumber = modelNumber; }

    public LocalDateTime getInstallationDate() { return installationDate; }
    public void setInstallationDate(LocalDateTime installationDate) { this.installationDate = installationDate; }

    public LocalDateTime getLastCalibrationDate() { return lastCalibrationDate; }
    public void setLastCalibrationDate(LocalDateTime lastCalibrationDate) { this.lastCalibrationDate = lastCalibrationDate; }

    public Double getCalibrationOffset() { return calibrationOffset; }
    public void setCalibrationOffset(Double calibrationOffset) { this.calibrationOffset = calibrationOffset; }

    public Double getLatestReadingValue() { return latestReadingValue; }
    public void setLatestReadingValue(Double latestReadingValue) { this.latestReadingValue = latestReadingValue; }

    public LocalDateTime getLatestReadingTime() { return latestReadingTime; }
    public void setLatestReadingTime(LocalDateTime latestReadingTime) { this.latestReadingTime = latestReadingTime; }
}
