package com.warehouse.riskmonitor.controller;

import com.warehouse.riskmonitor.dto.*;
import com.warehouse.riskmonitor.entity.*;
import com.warehouse.riskmonitor.entity.enums.*;
import com.warehouse.riskmonitor.generator.SyntheticDataGenerator;
import com.warehouse.riskmonitor.service.*;
import com.warehouse.riskmonitor.validation.ValidationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final DashboardService dashboardService;
    private final WarehouseService warehouseService;
    private final ZoneService zoneService;
    private final SensorService sensorService;
    private final TelemetryService telemetryService;
    private final DoorEventService doorEventService;
    private final InventoryLotService inventoryLotService;
    private final AlertService alertService;
    private final InspectionService inspectionService;
    private final RemediationService remediationService;
    private final SyntheticDataGenerator generator;
    private final ValidationService validationService;
    private final EvaluationService evaluationService;
    private final ExportService exportService;

    public ApiController(DashboardService dashboardService,
                         WarehouseService warehouseService,
                         ZoneService zoneService,
                         SensorService sensorService,
                         TelemetryService telemetryService,
                         DoorEventService doorEventService,
                         InventoryLotService inventoryLotService,
                         AlertService alertService,
                         InspectionService inspectionService,
                         RemediationService remediationService,
                         SyntheticDataGenerator generator,
                         ValidationService validationService,
                         EvaluationService evaluationService,
                         ExportService exportService) {
        this.dashboardService = dashboardService;
        this.warehouseService = warehouseService;
        this.zoneService = zoneService;
        this.sensorService = sensorService;
        this.telemetryService = telemetryService;
        this.doorEventService = doorEventService;
        this.inventoryLotService = inventoryLotService;
        this.alertService = alertService;
        this.inspectionService = inspectionService;
        this.remediationService = remediationService;
        this.generator = generator;
        this.validationService = validationService;
        this.evaluationService = evaluationService;
        this.exportService = exportService;
    }

    // --- Dashboard ---
    @GetMapping("/dashboard/stats")
    public DashboardStatsDto getDashboardStats() {
        return dashboardService.getDashboardStats();
    }

    // --- Warehouses ---
    @GetMapping("/warehouses")
    public List<Warehouse> getWarehouses() {
        return warehouseService.getAllWarehouses();
    }

    @PostMapping("/warehouses")
    public Warehouse createWarehouse(@RequestBody Warehouse warehouse) {
        return warehouseService.saveWarehouse(warehouse);
    }

    @PutMapping("/warehouses/{id}")
    public Warehouse updateWarehouse(@PathVariable String id, @RequestBody Warehouse warehouse) {
        return warehouseService.updateWarehouse(id, warehouse);
    }

    @DeleteMapping("/warehouses/{id}")
    public ResponseEntity<Void> deleteWarehouse(@PathVariable String id) {
        warehouseService.deleteWarehouse(id);
        return ResponseEntity.noContent().build();
    }

    // --- Zones ---
    @GetMapping("/zones")
    public List<Zone> getZones(@RequestParam(required = false) String warehouseId) {
        if (warehouseId != null && !warehouseId.trim().isEmpty()) {
            return zoneService.getZonesByWarehouse(warehouseId);
        }
        return zoneService.getAllZones();
    }

    @PostMapping("/zones")
    public Zone createZone(@RequestParam String warehouseId, @RequestBody Zone zone) {
        return zoneService.saveZone(zone, warehouseId);
    }

    @PutMapping("/zones/{id}")
    public Zone updateZone(@PathVariable String id, @RequestBody Zone zone) {
        return zoneService.updateZone(id, zone);
    }

    @DeleteMapping("/zones/{id}")
    public ResponseEntity<Void> deleteZone(@PathVariable String id) {
        zoneService.deleteZone(id);
        return ResponseEntity.noContent().build();
    }

    // --- Sensors ---
    @GetMapping("/sensors")
    public List<Sensor> getSensors() {
        return sensorService.getAllSensors();
    }

    @PostMapping("/sensors")
    public Sensor createSensor(@RequestParam String zoneId, @RequestBody Sensor sensor) {
        return sensorService.saveSensor(sensor, zoneId);
    }

    @PutMapping("/sensors/{id}")
    public Sensor updateSensor(@PathVariable String id, @RequestBody Sensor sensor) {
        return sensorService.updateSensor(id, sensor);
    }

    @DeleteMapping("/sensors/{id}")
    public ResponseEntity<Void> deleteSensor(@PathVariable String id) {
        sensorService.deleteSensor(id);
        return ResponseEntity.noContent().build();
    }

    // --- Telemetry & Time Series ---
    @GetMapping("/readings")
    public Page<SensorReading> getReadings(@RequestParam(defaultValue = "0") int page,
                                          @RequestParam(defaultValue = "50") int size,
                                          @RequestParam(required = false) String zoneId,
                                          @RequestParam(required = false) String sensorId,
                                          @RequestParam(required = false) SensorType type,
                                          @RequestParam(required = false) ReadingStatus status) {
        return telemetryService.getReadingsPaged(zoneId, sensorId, type, status, PageRequest.of(page, size));
    }

    @GetMapping("/readings/timeseries")
    public List<TimeSeriesPointDto> getTimeSeries(@RequestParam(required = false) String zoneId,
                                                 @RequestParam(defaultValue = "TEMPERATURE") SensorType type) {
        return telemetryService.getTimeSeries(zoneId, type);
    }

    // --- Door Events ---
    @GetMapping("/doors")
    public Page<DoorEvent> getDoorEvents(@RequestParam(defaultValue = "0") int page,
                                        @RequestParam(defaultValue = "50") int size,
                                        @RequestParam(required = false) String zoneId,
                                        @RequestParam(required = false) DoorStatus status) {
        return doorEventService.getDoorEventsPaged(zoneId, status, PageRequest.of(page, size));
    }

    @PostMapping("/doors")
    public DoorEvent createDoorEvent(@RequestParam(required = false) String zoneId,
                                    @RequestParam(required = false) String doorName,
                                    @RequestParam(required = false) String openedAt,
                                    @RequestParam(required = false) String closedAt,
                                    @RequestParam(required = false) Integer maxPermittedSeconds,
                                    @RequestBody(required = false) Map<String, Object> body) {
        if (body != null) {
            if (zoneId == null && body.containsKey("zoneId")) zoneId = (String) body.get("zoneId");
            if (doorName == null && body.containsKey("doorName")) doorName = (String) body.get("doorName");
            if (openedAt == null && body.containsKey("openedAt")) openedAt = (String) body.get("openedAt");
            if (closedAt == null && body.containsKey("closedAt")) closedAt = (String) body.get("closedAt");
            if (maxPermittedSeconds == null && body.containsKey("maxPermittedSeconds")) {
                maxPermittedSeconds = Integer.parseInt(body.get("maxPermittedSeconds").toString());
            }
        }
        if (zoneId == null || zoneId.trim().isEmpty()) {
            throw new IllegalArgumentException("zoneId is required");
        }
        if (doorName == null || doorName.trim().isEmpty()) {
            doorName = "Dock Door 1";
        }
        if (maxPermittedSeconds == null || maxPermittedSeconds <= 0) {
            maxPermittedSeconds = 180;
        }

        java.time.LocalDateTime openTime = java.time.LocalDateTime.now();
        if (openedAt != null && !openedAt.trim().isEmpty()) {
            try {
                openTime = java.time.OffsetDateTime.parse(openedAt).toLocalDateTime();
            } catch (Exception e) {
                try {
                    openTime = java.time.LocalDateTime.parse(openedAt.substring(0, Math.min(19, openedAt.length())));
                } catch (Exception ignored) {}
            }
        }

        java.time.LocalDateTime closeTime = null;
        if (closedAt != null && !closedAt.trim().isEmpty()) {
            try {
                closeTime = java.time.OffsetDateTime.parse(closedAt).toLocalDateTime();
            } catch (Exception e) {
                try {
                    closeTime = java.time.LocalDateTime.parse(closedAt.substring(0, Math.min(19, closedAt.length())));
                } catch (Exception ignored) {}
            }
        }

        return doorEventService.recordDoorEvent(zoneId, doorName, openTime, closeTime, maxPermittedSeconds);
    }

    // --- Inventory Lots ---
    @GetMapping("/inventory")
    public Page<InventoryLot> getInventory(@RequestParam(defaultValue = "0") int page,
                                          @RequestParam(defaultValue = "50") int size,
                                          @RequestParam(required = false) String zoneId,
                                          @RequestParam(required = false) LotStatus status,
                                          @RequestParam(required = false) ProductCategory category,
                                          @RequestParam(required = false) String search) {
        return inventoryLotService.getLotsPaged(zoneId, status, category, search, PageRequest.of(page, size));
    }

    @PostMapping("/inventory/{id}/quarantine")
    public InventoryLot quarantineLot(@PathVariable String id) {
        return inventoryLotService.updateLotStatus(id, LotStatus.QUARANTINED);
    }

    // --- Alerts ---
    @GetMapping("/alerts")
    public Page<Alert> getAlerts(@RequestParam(defaultValue = "0") int page,
                                @RequestParam(defaultValue = "50") int size,
                                @RequestParam(required = false) String zoneId,
                                @RequestParam(required = false) AlertStatus status,
                                @RequestParam(required = false) RiskLevel risk,
                                @RequestParam(required = false) AlertType type) {
        return alertService.getAlertsPaged(zoneId, status, risk, type, PageRequest.of(page, size));
    }

    @PostMapping("/alerts")
    public Alert createAlert(@RequestBody Map<String, Object> body) {
        String zoneId = (String) body.get("zoneId");
        AlertType type = AlertType.valueOf((String) body.getOrDefault("type", "HIGH_TEMPERATURE"));
        RiskLevel risk = RiskLevel.valueOf((String) body.getOrDefault("risk", "HIGH"));
        Double triggerVal = body.get("triggerValue") != null ? Double.valueOf(body.get("triggerValue").toString()) : 28.5;
        Double thresholdVal = body.get("thresholdValue") != null ? Double.valueOf(body.get("thresholdValue").toString()) : 24.0;
        String message = (String) body.getOrDefault("message", "Manual excursion alert triggered.");
        return alertService.createAlert(zoneId, type, risk, triggerVal, thresholdVal, message);
    }

    @PostMapping("/alerts/{id}/acknowledge")
    public Alert acknowledgeAlert(@PathVariable String id, @RequestBody(required = false) AlertActionDto action) {
        String user = action != null ? action.getUser() : "Admin";
        String notes = action != null ? action.getNotes() : "Acknowledged from Dashboard";
        return alertService.acknowledgeAlert(id, user, notes);
    }

    @PostMapping("/alerts/{id}/resolve")
    public Alert resolveAlert(@PathVariable String id, @RequestBody(required = false) AlertActionDto action) {
        String user = action != null ? action.getUser() : "Admin";
        String notes = action != null ? action.getNotes() : "Resolved from Dashboard";
        return alertService.resolveAlert(id, user, notes);
    }

    // --- Inspections ---
    @GetMapping("/inspections")
    public Page<Inspection> getInspections(@RequestParam(defaultValue = "0") int page,
                                          @RequestParam(defaultValue = "50") int size,
                                          @RequestParam(required = false) String zoneId,
                                          @RequestParam(required = false) InspectionStatus status) {
        return inspectionService.getInspectionsPaged(zoneId, status, PageRequest.of(page, size));
    }

    @PostMapping("/inspections/{id}/complete")
    public Inspection completeInspection(@PathVariable String id, @RequestBody Map<String, String> body) {
        return inspectionService.completeInspection(id, body.getOrDefault("findings", "Inspection completed with findings."));
    }

    // --- Remediations ---
    @GetMapping("/remediations")
    public Page<Remediation> getRemediations(@RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "50") int size,
                                            @RequestParam(required = false) RemediationStatus status,
                                            @RequestParam(required = false) RemediationType type) {
        return remediationService.getRemediationsPaged(status, type, PageRequest.of(page, size));
    }

    @PostMapping("/remediations/{id}/complete")
    public Remediation completeRemediation(@PathVariable String id, @RequestBody Map<String, String> body) {
        return remediationService.completeRemediation(id, body.getOrDefault("notes", "Corrective action completed."));
    }

    // --- Generator Runner ---
    @PostMapping("/generator/run")
    public Map<String, Object> runGenerator(@RequestBody GenerationRequestDto req) {
        long start = System.currentTimeMillis();
        SyntheticDataGenerator.GeneratedDataset dataset = generator.generate(req.getScale(), req.getSeed(), true);
        if (req.isExportFiles()) {
            exportService.exportAllToDataDirectory();
        }
        long duration = System.currentTimeMillis() - start;

        Map<String, Object> resp = new HashMap<>();
        resp.put("status", "SUCCESS");
        resp.put("scale", req.getScale());
        resp.put("seed", req.getSeed());
        resp.put("durationMs", duration);
        resp.put("warehouses", dataset.warehouses.size());
        resp.put("zones", dataset.zones.size());
        resp.put("sensors", dataset.sensors.size());
        resp.put("lots", dataset.inventoryLots.size());
        resp.put("readings", dataset.readings.size());
        resp.put("doorEvents", dataset.doorEvents.size());
        resp.put("alerts", dataset.alerts.size());
        resp.put("inspections", dataset.inspections.size());
        resp.put("remediations", dataset.remediations.size());
        resp.put("groundTruthAnomalies", dataset.groundTruthAnomalies.size());
        return resp;
    }

    // --- Validation Suite Runner ---
    @GetMapping("/validation/run")
    public ValidationSummaryDto runValidation() {
        return validationService.executeFullValidation();
    }

    // --- Evaluation Metrics ---
    @GetMapping("/evaluation/metrics")
    public EvaluationMetricsDto getEvaluationMetrics() {
        return evaluationService.evaluateAgainstGroundTruth();
    }

    // --- Data Export Download ---
    @GetMapping("/export/{format}/{entity}")
    public ResponseEntity<byte[]> exportData(@PathVariable String format, @PathVariable String entity) {
        String data;
        String contentType;
        String filename = entity + "." + format.toLowerCase();

        if ("csv".equalsIgnoreCase(format)) {
            data = exportService.exportEntityToCsv(entity);
            contentType = "text/csv; charset=UTF-8";
        } else if ("json".equalsIgnoreCase(format)) {
            data = exportService.exportEntityToJson(entity);
            contentType = "application/json; charset=UTF-8";
        } else if ("sql".equalsIgnoreCase(format)) {
            data = exportService.exportEntityToSql(entity);
            contentType = "application/sql; charset=UTF-8";
        } else {
            throw new IllegalArgumentException("Unsupported export format: " + format);
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType(contentType))
                .body(data.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
}
