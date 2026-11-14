package com.warehouse.riskmonitor.dto;

import com.warehouse.riskmonitor.entity.Alert;
import com.warehouse.riskmonitor.entity.SensorReading;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DashboardStatsDto {
    private long totalWarehouses;
    private long totalZones;
    private long totalSensors;
    private long totalLots;
    private long activeAlerts;
    private long criticalAlerts;
    private long normalZones;
    private long attentionZones;
    private long compromisedLots;
    private double totalRemediationCost;

    private Map<String, Long> riskDistribution = new HashMap<>();
    private Map<String, Long> sensorTypeDistribution = new HashMap<>();
    private List<Alert> recentAlerts;
    private List<SensorReading> recentReadings;

    public DashboardStatsDto() {}

    public long getTotalWarehouses() { return totalWarehouses; }
    public void setTotalWarehouses(long totalWarehouses) { this.totalWarehouses = totalWarehouses; }

    public long getTotalZones() { return totalZones; }
    public void setTotalZones(long totalZones) { this.totalZones = totalZones; }

    public long getTotalSensors() { return totalSensors; }
    public void setTotalSensors(long totalSensors) { this.totalSensors = totalSensors; }

    public long getTotalLots() { return totalLots; }
    public void setTotalLots(long totalLots) { this.totalLots = totalLots; }

    public long getActiveAlerts() { return activeAlerts; }
    public void setActiveAlerts(long activeAlerts) { this.activeAlerts = activeAlerts; }

    public long getCriticalAlerts() { return criticalAlerts; }
    public void setCriticalAlerts(long criticalAlerts) { this.criticalAlerts = criticalAlerts; }

    public long getNormalZones() { return normalZones; }
    public void setNormalZones(long normalZones) { this.normalZones = normalZones; }

    public long getAttentionZones() { return attentionZones; }
    public void setAttentionZones(long attentionZones) { this.attentionZones = attentionZones; }

    public long getCompromisedLots() { return compromisedLots; }
    public void setCompromisedLots(long compromisedLots) { this.compromisedLots = compromisedLots; }

    public double getTotalRemediationCost() { return totalRemediationCost; }
    public void setTotalRemediationCost(double totalRemediationCost) { this.totalRemediationCost = totalRemediationCost; }

    public Map<String, Long> getRiskDistribution() { return riskDistribution; }
    public void setRiskDistribution(Map<String, Long> riskDistribution) { this.riskDistribution = riskDistribution; }

    public Map<String, Long> getSensorTypeDistribution() { return sensorTypeDistribution; }
    public void setSensorTypeDistribution(Map<String, Long> sensorTypeDistribution) { this.sensorTypeDistribution = sensorTypeDistribution; }

    public List<Alert> getRecentAlerts() { return recentAlerts; }
    public void setRecentAlerts(List<Alert> recentAlerts) { this.recentAlerts = recentAlerts; }

    public List<SensorReading> getRecentReadings() { return recentReadings; }
    public void setRecentReadings(List<SensorReading> recentReadings) { this.recentReadings = recentReadings; }
}

