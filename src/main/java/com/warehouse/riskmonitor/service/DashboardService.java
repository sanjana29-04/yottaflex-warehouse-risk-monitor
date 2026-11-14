package com.warehouse.riskmonitor.service;

import com.warehouse.riskmonitor.dto.DashboardStatsDto;
import com.warehouse.riskmonitor.entity.enums.AlertStatus;
import com.warehouse.riskmonitor.entity.enums.ZoneStatus;
import com.warehouse.riskmonitor.repository.*;
import org.springframework.stereotype.Service;

@Service
public class DashboardService {

    private final WarehouseRepository warehouseRepository;
    private final ZoneRepository zoneRepository;
    private final SensorRepository sensorRepository;
    private final InventoryLotRepository inventoryLotRepository;
    private final AlertService alertService;
    private final SensorService sensorService;
    private final TelemetryService telemetryService;
    private final RemediationService remediationService;
    private volatile DashboardStatsDto cachedStats;
    private volatile long lastStatsFetch = 0;

    public DashboardService(WarehouseRepository warehouseRepository,
                            ZoneRepository zoneRepository,
                            SensorRepository sensorRepository,
                            InventoryLotRepository inventoryLotRepository,
                            AlertService alertService,
                            SensorService sensorService,
                            TelemetryService telemetryService,
                            RemediationService remediationService) {
        this.warehouseRepository = warehouseRepository;
        this.zoneRepository = zoneRepository;
        this.sensorRepository = sensorRepository;
        this.inventoryLotRepository = inventoryLotRepository;
        this.alertService = alertService;
        this.sensorService = sensorService;
        this.telemetryService = telemetryService;
        this.remediationService = remediationService;
    }

    public DashboardStatsDto getDashboardStats() {
        if (cachedStats != null && (System.currentTimeMillis() - lastStatsFetch < 4000)) {
            return cachedStats;
        }
        DashboardStatsDto stats = new DashboardStatsDto();

        stats.setTotalWarehouses(warehouseRepository.count());
        stats.setTotalZones(zoneRepository.count());
        stats.setTotalSensors(sensorRepository.count());
        stats.setTotalLots(inventoryLotRepository.count());

        long openAlerts = alertService.getCountByStatus(AlertStatus.OPEN);
        long ackAlerts = alertService.getCountByStatus(AlertStatus.ACKNOWLEDGED);
        stats.setActiveAlerts(openAlerts + ackAlerts);
        stats.setCriticalAlerts(alertService.getCriticalAlertsCount());

        long normalZones = zoneRepository.countByStatus(ZoneStatus.NORMAL);
        long warnZones = zoneRepository.countByStatus(ZoneStatus.WARNING);
        long critZones = zoneRepository.countByStatus(ZoneStatus.CRITICAL);
        stats.setNormalZones(normalZones);
        stats.setAttentionZones(warnZones + critZones);

        stats.setCompromisedLots(inventoryLotRepository.countCompromisedLots());
        stats.setTotalRemediationCost(remediationService.getTotalCostImpact());

        stats.setRiskDistribution(alertService.getRiskDistribution());
        stats.setSensorTypeDistribution(sensorService.getSensorTypeCounts());

        stats.setRecentAlerts(alertService.getRecentAlerts(6));
        stats.setRecentReadings(telemetryService.getRecentReadings(8));

        cachedStats = stats;
        lastStatsFetch = System.currentTimeMillis();

        return stats;
    }
}

