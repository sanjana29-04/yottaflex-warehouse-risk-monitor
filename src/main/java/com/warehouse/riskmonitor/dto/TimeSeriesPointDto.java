package com.warehouse.riskmonitor.dto;

import java.time.LocalDateTime;

public class TimeSeriesPointDto {
    private LocalDateTime timestamp;
    private double value;
    private String sensorId;
    private String zoneId;
    private String sensorType;
    private String status;
    private boolean isAnomaly;

    public TimeSeriesPointDto() {}

    public TimeSeriesPointDto(LocalDateTime timestamp, double value, String sensorId, String zoneId, String sensorType, String status, boolean isAnomaly) {
        this.timestamp = timestamp;
        this.value = value;
        this.sensorId = sensorId;
        this.zoneId = zoneId;
        this.sensorType = sensorType;
        this.status = status;
        this.isAnomaly = isAnomaly;
    }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public double getValue() { return value; }
    public void setValue(double value) { this.value = value; }

    public String getSensorId() { return sensorId; }
    public void setSensorId(String sensorId) { this.sensorId = sensorId; }

    public String getZoneId() { return zoneId; }
    public void setZoneId(String zoneId) { this.zoneId = zoneId; }

    public String getSensorType() { return sensorType; }
    public void setSensorType(String sensorType) { this.sensorType = sensorType; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public boolean isAnomaly() { return isAnomaly; }
    public void setAnomaly(boolean anomaly) { isAnomaly = anomaly; }
}

