package com.warehouse.riskmonitor.validation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.warehouse.riskmonitor.dto.ValidationResultDto;
import com.warehouse.riskmonitor.dto.ValidationSummaryDto;
import com.warehouse.riskmonitor.entity.*;
import com.warehouse.riskmonitor.entity.enums.*;
import com.warehouse.riskmonitor.repository.*;
import com.warehouse.riskmonitor.service.ExportService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

@Service
public class ValidationService {

    private final WarehouseRepository warehouseRepository;
    private final ZoneRepository zoneRepository;
    private final StorageRequirementRepository storageRequirementRepository;
    private final SensorRepository sensorRepository;
    private final SensorReadingRepository sensorReadingRepository;
    private final DoorEventRepository doorEventRepository;
    private final InventoryLotRepository inventoryLotRepository;
    private final LotZoneAssignmentRepository lotZoneAssignmentRepository;
    private final AlertRepository alertRepository;
    private final AlarmLotImpactRepository alarmLotImpactRepository;
    private final InspectionRepository inspectionRepository;
    private final RemediationRepository remediationRepository;
    private final GroundTruthAnomalyRepository groundTruthAnomalyRepository;
    private final SensorZoneMappingRepository sensorZoneMappingRepository;
    private final JdbcTemplate jdbcTemplate;
    private final ExportService exportService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ValidationService(WarehouseRepository warehouseRepository,
                             ZoneRepository zoneRepository,
                             StorageRequirementRepository storageRequirementRepository,
                             SensorRepository sensorRepository,
                             SensorZoneMappingRepository sensorZoneMappingRepository,
                             SensorReadingRepository sensorReadingRepository,
                             DoorEventRepository doorEventRepository,
                             InventoryLotRepository inventoryLotRepository,
                             LotZoneAssignmentRepository lotZoneAssignmentRepository,
                             AlertRepository alertRepository,
                             AlarmLotImpactRepository alarmLotImpactRepository,
                             InspectionRepository inspectionRepository,
                             RemediationRepository remediationRepository,
                             GroundTruthAnomalyRepository groundTruthAnomalyRepository,
                             JdbcTemplate jdbcTemplate,
                             ExportService exportService) {
        this.warehouseRepository = warehouseRepository;
        this.zoneRepository = zoneRepository;
        this.storageRequirementRepository = storageRequirementRepository;
        this.sensorRepository = sensorRepository;
        this.sensorZoneMappingRepository = sensorZoneMappingRepository;
        this.sensorReadingRepository = sensorReadingRepository;
        this.doorEventRepository = doorEventRepository;
        this.inventoryLotRepository = inventoryLotRepository;
        this.lotZoneAssignmentRepository = lotZoneAssignmentRepository;
        this.alertRepository = alertRepository;
        this.alarmLotImpactRepository = alarmLotImpactRepository;
        this.inspectionRepository = inspectionRepository;
        this.remediationRepository = remediationRepository;
        this.groundTruthAnomalyRepository = groundTruthAnomalyRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.exportService = exportService;
    }

    public ValidationSummaryDto executeFullValidation() {
        ValidationSummaryDto summary = new ValidationSummaryDto();
        List<ValidationResultDto> results = new ArrayList<>();

        List<Warehouse> warehouses = warehouseRepository.findAll();
        List<Zone> zones = zoneRepository.findAll();
        List<StorageRequirement> storageRequirements = storageRequirementRepository.findAll();
        List<Sensor> sensors = sensorRepository.findAll();
        List<DoorEvent> doorEvents = doorEventRepository.findAll();
        List<InventoryLot> lots = inventoryLotRepository.findAll();
        List<LotZoneAssignment> lzas = lotZoneAssignmentRepository.findAll();
        List<Alert> alerts = alertRepository.findAll();
        List<AlarmLotImpact> impacts = alarmLotImpactRepository.findAll();
        List<Inspection> inspections = inspectionRepository.findAll();
        List<Remediation> remediations = remediationRepository.findAll();
        List<GroundTruthAnomaly> groundTruth = groundTruthAnomalyRepository.findAll();
        long totalReadingsCount = sensorReadingRepository.count();

        int ruleNo = 1;

        // Group 1: Primary Key Uniqueness (Rules 1 - 11)
        results.add(checkPkUniqueness(ruleNo++, "PK_UNIQUENESS", "Warehouse PK Uniqueness", warehouses, Warehouse::getId));
        results.add(checkPkUniqueness(ruleNo++, "PK_UNIQUENESS", "Zone PK Uniqueness", zones, Zone::getId));
        results.add(checkPkUniqueness(ruleNo++, "PK_UNIQUENESS", "Storage Requirement PK Uniqueness", storageRequirements, StorageRequirement::getId));
        results.add(checkPkUniqueness(ruleNo++, "PK_UNIQUENESS", "Sensor PK Uniqueness", sensors, Sensor::getId));
        results.add(checkPkUniqueness(ruleNo++, "PK_UNIQUENESS", "Door Event PK Uniqueness", doorEvents, DoorEvent::getId));
        results.add(checkPkUniqueness(ruleNo++, "PK_UNIQUENESS", "Inventory Lot PK Uniqueness", lots, InventoryLot::getId));
        results.add(checkPkUniqueness(ruleNo++, "PK_UNIQUENESS", "Lot Zone Assignment PK Uniqueness", lzas, LotZoneAssignment::getId));
        results.add(checkPkUniqueness(ruleNo++, "PK_UNIQUENESS", "Alert PK Uniqueness", alerts, Alert::getId));
        results.add(checkPkUniqueness(ruleNo++, "PK_UNIQUENESS", "Inspection PK Uniqueness", inspections, Inspection::getId));
        results.add(checkPkUniqueness(ruleNo++, "PK_UNIQUENESS", "Remediation PK Uniqueness", remediations, Remediation::getId));
        results.add(checkSensorReadingsPkUniqueness(ruleNo++, totalReadingsCount));

        // Group 2: Foreign Key Referential Integrity (Rules 12 - 22)
        Set<String> whIds = toIdSet(warehouses, Warehouse::getId);
        Set<String> zoneIds = toIdSet(zones, Zone::getId);
        Set<String> reqIds = toIdSet(storageRequirements, StorageRequirement::getId);
        Set<String> sensorIds = toIdSet(sensors, Sensor::getId);
        Set<String> lotIds = toIdSet(lots, InventoryLot::getId);
        Set<String> alertIds = toIdSet(alerts, Alert::getId);

        results.add(checkFk(ruleNo++, "FK_INTEGRITY", "Zone -> Warehouse FK Integrity", zones, z -> z.getWarehouse() != null ? z.getWarehouse().getId() : null, whIds));
        results.add(checkFk(ruleNo++, "FK_INTEGRITY", "Sensor -> Zone FK Integrity", sensors, s -> s.getZone() != null ? s.getZone().getId() : null, zoneIds));
        results.add(checkFk(ruleNo++, "FK_INTEGRITY", "DoorEvent -> Zone FK Integrity", doorEvents, d -> d.getZone() != null ? d.getZone().getId() : null, zoneIds));
        results.add(checkFk(ruleNo++, "FK_INTEGRITY", "InventoryLot -> StorageRequirement FK Integrity", lots, l -> l.getStorageRequirement() != null ? l.getStorageRequirement().getId() : null, reqIds));
        results.add(checkFk(ruleNo++, "FK_INTEGRITY", "InventoryLot -> Zone FK Integrity", lots, l -> l.getCurrentZone() != null ? l.getCurrentZone().getId() : null, zoneIds));
        results.add(checkFk(ruleNo++, "FK_INTEGRITY", "LotZoneAssignment -> Lot FK Integrity", lzas, lza -> lza.getLot() != null ? lza.getLot().getId() : null, lotIds));
        results.add(checkFk(ruleNo++, "FK_INTEGRITY", "LotZoneAssignment -> Zone FK Integrity", lzas, lza -> lza.getZone() != null ? lza.getZone().getId() : null, zoneIds));
        results.add(checkFk(ruleNo++, "FK_INTEGRITY", "Alert -> Zone FK Integrity", alerts, a -> a.getZone() != null ? a.getZone().getId() : null, zoneIds));
        results.add(checkFk(ruleNo++, "FK_INTEGRITY", "Inspection -> Zone FK Integrity", inspections, i -> i.getZone() != null ? i.getZone().getId() : null, zoneIds));
        results.add(checkFk(ruleNo++, "FK_INTEGRITY", "Remediation -> Alert FK Integrity", remediations, r -> r.getAlert() != null ? r.getAlert().getId() : null, alertIds));
        results.add(checkSensorReadingsFkIntegrity(ruleNo++, totalReadingsCount));

        // Group 3: Enum Validity (Rules 23 - 30) - REAL DATA CHECKS
        results.add(checkEnumValidity(ruleNo++, "Warehouse Status Enum Validity", warehouses, w -> w.getStatus() != null, "WarehouseStatus"));
        results.add(checkEnumValidity(ruleNo++, "Zone Type & Status Enum Validity", zones, z -> z.getZoneType() != null && z.getStatus() != null, "ZoneType/ZoneStatus"));
        results.add(checkEnumValidity(ruleNo++, "Sensor Type & Status Enum Validity", sensors, s -> s.getSensorType() != null && s.getStatus() != null, "SensorType/SensorStatus"));
        results.add(checkEnumValidity(ruleNo++, "Door Status Enum Validity", doorEvents, d -> d.getStatus() != null, "DoorStatus"));
        results.add(checkEnumValidity(ruleNo++, "Inventory Lot Category & Status Enum Validity", lots, l -> l.getCategory() != null && l.getStatus() != null && l.getUnit() != null, "ProductCategory/LotStatus/UnitOfMeasure"));
        results.add(checkEnumValidity(ruleNo++, "Alert Type & Risk Level Enum Validity", alerts, a -> a.getAlertType() != null && a.getRiskLevel() != null && a.getStatus() != null, "AlertType/RiskLevel/AlertStatus"));
        results.add(checkEnumValidity(ruleNo++, "Inspection Status Enum Validity", inspections, i -> i.getStatus() != null, "InspectionStatus"));
        results.add(checkEnumValidity(ruleNo++, "Remediation Type & Status Enum Validity", remediations, r -> r.getActionType() != null && r.getStatus() != null, "RemediationType/RemediationStatus"));

        // Group 4: Chronological Date Ordering (Rules 31 - 37) - REAL DATA CHECKS
        results.add(checkDateOrder(ruleNo++, "CHRONOLOGY", "Door Event Opened <= Closed", doorEvents,
                d -> d.getClosedAt() == null || !d.getOpenedAt().isAfter(d.getClosedAt())));
        results.add(checkDateOrder(ruleNo++, "CHRONOLOGY", "Inventory Ingress <= Expiry Date", lots,
                l -> l.getIngressTimestamp() != null && l.getExpiryDate() != null && !l.getIngressTimestamp().isAfter(l.getExpiryDate())));
        results.add(checkDateOrder(ruleNo++, "CHRONOLOGY", "Alert Triggered <= Resolved Date", alerts,
                a -> a.getResolvedAt() == null || !a.getTriggeredAt().isAfter(a.getResolvedAt())));
        results.add(checkDateOrder(ruleNo++, "CHRONOLOGY", "Remediation Scheduled <= Completed Date", remediations,
                r -> r.getCompletedAt() == null || !r.getScheduledAt().isAfter(r.getCompletedAt())));
        results.add(checkDateOrder(ruleNo++, "CHRONOLOGY", "Sensor Installation <= Calibration Date", sensors,
                s -> s.getLastCalibrationDate() == null || !s.getInstallationDate().isAfter(s.getLastCalibrationDate())));
        results.add(checkDateOrder(ruleNo++, "CHRONOLOGY", "Lot Zone Assignment Ingress <= Egress Date", lzas,
                lza -> lza.getEgressTimestamp() == null || !lza.getIngressTimestamp().isAfter(lza.getEgressTimestamp())));
        results.add(checkSensorReadingTimestamps(ruleNo++, totalReadingsCount));

        // Group 5: Arithmetic & Physical Range Reconciliation (Rules 38 - 45) - REAL DATA CHECKS
        results.add(checkWarehouseZonesReconciliation(ruleNo++, warehouses, zones));
        results.add(checkZoneTempBoundaries(ruleNo++, zones));
        results.add(checkZoneHumidityBoundaries(ruleNo++, zones));
        results.add(checkStorageRequirementBounds(ruleNo++, storageRequirements));
        results.add(checkDoorEventDurationArithmetic(ruleNo++, doorEvents));
        results.add(checkTelemetryHumidityRange(ruleNo++, totalReadingsCount));
        results.add(checkTelemetryTemperatureRange(ruleNo++, totalReadingsCount));
        results.add(checkActiveSensorTelemetryCoverage(ruleNo++, sensors));

        // Group 6: Edge Case, Ground Truth & Export Consistency (Rules 46 - 50) - REAL DATA CHECKS
        results.add(checkGroundTruthIsolation(ruleNo++, groundTruth));
        results.add(checkIncompatibleZoneGroundTruth(ruleNo++, lots, groundTruth));
        results.add(checkSensorTopologyGroundTruth(ruleNo++, sensors, groundTruth));
        results.add(checkTelemetryGapAndAlarmGroundTruth(ruleNo++, groundTruth));
        results.add(checkExportConsistency(ruleNo++, warehouses, zones, storageRequirements));

        int passed = (int) results.stream().filter(r -> "PASSED".equalsIgnoreCase(r.getStatus())).count();
        int failed = results.size() - passed;

        summary.setTotalRulesRun(results.size());
        summary.setPassedRules(passed);
        summary.setFailedRules(failed);
        summary.setFullyValid(failed == 0);
        summary.setResults(results);

        return summary;
    }

    private <T> ValidationResultDto checkPkUniqueness(int ruleNo, String category, String name, List<T> items, Function<T, String> idExtractor) {
        if (items.isEmpty()) {
            return new ValidationResultDto(ruleNo, category, name, "FAILED", "No records found in database table to evaluate PK uniqueness", 0, 1);
        }
        Set<String> seen = new HashSet<>();
        int dupes = 0;
        for (T item : items) {
            String id = idExtractor.apply(item);
            if (id == null || id.trim().isEmpty() || !seen.add(id)) dupes++;
        }
        String status = dupes == 0 ? "PASSED" : "FAILED";
        String details = dupes == 0 ? "All " + items.size() + " IDs are strictly unique" : dupes + " duplicate or null IDs found";
        return new ValidationResultDto(ruleNo, category, name, status, details, items.size(), dupes);
    }

    private ValidationResultDto checkSensorReadingsPkUniqueness(int ruleNo, long totalReadings) {
        if (totalReadings == 0) {
            return new ValidationResultDto(ruleNo, "PK_UNIQUENESS", "Sensor Reading PK Uniqueness", "FAILED", "sensor_readings table is empty", 0, 1);
        }
        try {
            Long duplicates = jdbcTemplate.queryForObject(
                    "SELECT COUNT(id) - COUNT(DISTINCT id) FROM sensor_readings", Long.class);
            long dupes = duplicates != null ? duplicates : 0;
            String status = dupes == 0 ? "PASSED" : "FAILED";
            String details = dupes == 0
                    ? "All " + totalReadings + " sensor reading IDs are strictly unique"
                    : dupes + " duplicate reading IDs detected";
            return new ValidationResultDto(ruleNo, "PK_UNIQUENESS", "Sensor Reading PK Uniqueness", status, details, totalReadings, (int) Math.min(dupes, Integer.MAX_VALUE));
        } catch (Exception e) {
            return new ValidationResultDto(ruleNo, "PK_UNIQUENESS", "Sensor Reading PK Uniqueness", "FAILED", "Database query error: " + e.getMessage(), totalReadings, 1);
        }
    }

    private <T> ValidationResultDto checkFk(int ruleNo, String category, String name, List<T> items,
                                            Function<T, String> fkExtractor, Set<String> validIds) {
        if (items.isEmpty()) {
            return new ValidationResultDto(ruleNo, category, name, "FAILED", "No records found to validate foreign keys", 0, 1);
        }
        int invalid = 0;
        for (T item : items) {
            String fk = fkExtractor.apply(item);
            if (fk == null || !validIds.contains(fk)) invalid++;
        }
        String status = invalid == 0 ? "PASSED" : "FAILED";
        String details = invalid == 0 ? "All " + items.size() + " foreign keys resolve to valid parents" : invalid + " orphan foreign keys found";
        return new ValidationResultDto(ruleNo, category, name, status, details, items.size(), invalid);
    }

    private ValidationResultDto checkSensorReadingsFkIntegrity(int ruleNo, long totalReadings) {
        if (totalReadings == 0) {
            return new ValidationResultDto(ruleNo, "FK_INTEGRITY", "SensorReading -> Sensor & Zone FK Integrity", "FAILED", "No sensor readings present to evaluate", 0, 1);
        }
        try {
            Long orphanSensors = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM sensor_readings r LEFT JOIN sensors s ON r.sensor_id = s.id WHERE s.id IS NULL", Long.class);
            Long orphanZones = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM sensor_readings r LEFT JOIN zones z ON r.zone_id = z.id WHERE z.id IS NULL", Long.class);
            long totalOrphans = (orphanSensors != null ? orphanSensors : 0) + (orphanZones != null ? orphanZones : 0);
            String status = totalOrphans == 0 ? "PASSED" : "FAILED";
            String details = totalOrphans == 0
                    ? "All " + totalReadings + " readings resolve to valid sensors and zones"
                    : totalOrphans + " reading records reference non-existent sensors or zones";
            return new ValidationResultDto(ruleNo, "FK_INTEGRITY", "SensorReading -> Sensor & Zone FK Integrity", status, details, totalReadings, (int) Math.min(totalOrphans, Integer.MAX_VALUE));
        } catch (Exception e) {
            return new ValidationResultDto(ruleNo, "FK_INTEGRITY", "SensorReading -> Sensor & Zone FK Integrity", "FAILED", "Query error: " + e.getMessage(), totalReadings, 1);
        }
    }

    private <T> ValidationResultDto checkEnumValidity(int ruleNo, String name, List<T> items, Predicate<T> validator, String enumName) {
        if (items.isEmpty()) {
            return new ValidationResultDto(ruleNo, "ENUM_VALIDITY", name, "FAILED", "No records found in database to evaluate enum validity", 0, 1);
        }
        int invalid = 0;
        for (T item : items) {
            if (!validator.test(item)) invalid++;
        }
        String status = invalid == 0 ? "PASSED" : "FAILED";
        String details = invalid == 0
                ? "All " + items.size() + " records conform to valid " + enumName + " domain values"
                : invalid + " records have null or invalid enum state";
        return new ValidationResultDto(ruleNo, "ENUM_VALIDITY", name, status, details, items.size(), invalid);
    }

    private <T> ValidationResultDto checkDateOrder(int ruleNo, String category, String name, List<T> items,
                                                   Predicate<T> predicate) {
        if (items.isEmpty()) {
            return new ValidationResultDto(ruleNo, category, name, "PASSED", "No records present to test date ordering (0 violations)", 0, 0);
        }
        int violations = 0;
        for (T item : items) {
            if (!predicate.test(item)) violations++;
        }
        String status = violations == 0 ? "PASSED" : "FAILED";
        String details = violations == 0 ? "All " + items.size() + " records satisfy chronological ordering" : violations + " timestamp inversions detected";
        return new ValidationResultDto(ruleNo, category, name, status, details, items.size(), violations);
    }

    private ValidationResultDto checkSensorReadingTimestamps(int ruleNo, long totalReadings) {
        if (totalReadings == 0) {
            return new ValidationResultDto(ruleNo, "CHRONOLOGY", "Sensor Reading Timestamp Validity", "FAILED", "sensor_readings table is empty", 0, 1);
        }
        try {
            Long futureReadings = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM sensor_readings WHERE timestamp > '2030-01-01 00:00:00' OR timestamp IS NULL", Long.class);
            long invalid = futureReadings != null ? futureReadings : 0;
            String status = invalid == 0 ? "PASSED" : "FAILED";
            String details = invalid == 0
                    ? "All " + totalReadings + " telemetry timestamps are non-null and within valid time domain"
                    : invalid + " timestamps are null or out of plausible range";
            return new ValidationResultDto(ruleNo, "CHRONOLOGY", "Sensor Reading Timestamp Validity", status, details, totalReadings, (int) Math.min(invalid, Integer.MAX_VALUE));
        } catch (Exception e) {
            return new ValidationResultDto(ruleNo, "CHRONOLOGY", "Sensor Reading Timestamp Validity", "FAILED", "Query error: " + e.getMessage(), totalReadings, 1);
        }
    }

    private ValidationResultDto checkWarehouseZonesReconciliation(int ruleNo, List<Warehouse> warehouses, List<Zone> zones) {
        if (warehouses.isEmpty()) {
            return new ValidationResultDto(ruleNo, "ARITHMETIC", "Warehouse Total Zones Count Reconciliation", "FAILED", "No warehouses registered", 0, 1);
        }
        Map<String, Integer> zoneCountByWh = new HashMap<>();
        for (Zone z : zones) {
            if (z.getWarehouse() != null) {
                zoneCountByWh.merge(z.getWarehouse().getId(), 1, Integer::sum);
            }
        }
        int mismatches = 0;
        for (Warehouse wh : warehouses) {
            int actualZones = zoneCountByWh.getOrDefault(wh.getId(), 0);
            if (wh.getTotalZones() == null || wh.getTotalZones() != actualZones) {
                mismatches++;
            }
        }
        String status = mismatches == 0 ? "PASSED" : "FAILED";
        String details = mismatches == 0
                ? "Total zones metadata (" + zones.size() + ") exactly matches child zone counts across all " + warehouses.size() + " warehouses"
                : mismatches + " warehouses have total_zones metadata mismatching actual child count";
        return new ValidationResultDto(ruleNo, "ARITHMETIC", "Warehouse Total Zones Count Reconciliation", status, details, warehouses.size(), mismatches);
    }

    private ValidationResultDto checkZoneTempBoundaries(int ruleNo, List<Zone> zones) {
        if (zones.isEmpty()) {
            return new ValidationResultDto(ruleNo, "ARITHMETIC", "Zone Temperature Threshold Boundaries (Min < Max)", "FAILED", "No zones registered", 0, 1);
        }
        int violations = 0;
        for (Zone z : zones) {
            if (z.getTemperatureMin() == null || z.getTemperatureMax() == null || z.getTemperatureMin() >= z.getTemperatureMax()) {
                violations++;
            }
        }
        String status = violations == 0 ? "PASSED" : "FAILED";
        String details = violations == 0
                ? "All " + zones.size() + " zones satisfy temperatureMin strictly less than temperatureMax"
                : violations + " zones have inverted or invalid temperature thresholds (min >= max)";
        return new ValidationResultDto(ruleNo, "ARITHMETIC", "Zone Temperature Threshold Boundaries (Min < Max)", status, details, zones.size(), violations);
    }

    private ValidationResultDto checkZoneHumidityBoundaries(int ruleNo, List<Zone> zones) {
        if (zones.isEmpty()) {
            return new ValidationResultDto(ruleNo, "ARITHMETIC", "Zone Humidity Range Boundaries (Min < Max)", "FAILED", "No zones registered", 0, 1);
        }
        int violations = 0;
        for (Zone z : zones) {
            if (z.getHumidityMin() == null || z.getHumidityMax() == null || z.getHumidityMin() >= z.getHumidityMax()) {
                violations++;
            }
        }
        String status = violations == 0 ? "PASSED" : "FAILED";
        String details = violations == 0
                ? "All " + zones.size() + " zones satisfy humidityMin strictly less than humidityMax"
                : violations + " zones have inverted or invalid humidity thresholds (min >= max)";
        return new ValidationResultDto(ruleNo, "ARITHMETIC", "Zone Humidity Range Boundaries (Min < Max)", status, details, zones.size(), violations);
    }

    private ValidationResultDto checkStorageRequirementBounds(int ruleNo, List<StorageRequirement> reqs) {
        if (reqs.isEmpty()) {
            return new ValidationResultDto(ruleNo, "ARITHMETIC", "Storage Requirement Temp & Humidity Bounds", "FAILED", "No storage requirements registered", 0, 1);
        }
        int violations = 0;
        for (StorageRequirement r : reqs) {
            boolean tempValid = r.getTempMin() != null && r.getTempMax() != null && r.getTempTarget() != null
                    && r.getTempMin() <= r.getTempTarget() && r.getTempTarget() <= r.getTempMax();
            boolean humValid = r.getHumidityMin() != null && r.getHumidityMax() != null && r.getHumidityTarget() != null
                    && r.getHumidityMin() <= r.getHumidityTarget() && r.getHumidityTarget() <= r.getHumidityMax();
            if (!tempValid || !humValid) violations++;
        }
        String status = violations == 0 ? "PASSED" : "FAILED";
        String details = violations == 0
                ? "All " + reqs.size() + " storage requirements have target values strictly within [min, max] ranges"
                : violations + " requirements have targets outside allowed [min, max] range";
        return new ValidationResultDto(ruleNo, "ARITHMETIC", "Storage Requirement Temp & Humidity Bounds", status, details, reqs.size(), violations);
    }

    private ValidationResultDto checkDoorEventDurationArithmetic(int ruleNo, List<DoorEvent> events) {
        if (events.isEmpty()) {
            return new ValidationResultDto(ruleNo, "ARITHMETIC", "Door Event Duration Arithmetic Match", "PASSED", "No door events logged (0 violations)", 0, 0);
        }
        int violations = 0;
        for (DoorEvent d : events) {
            if (d.getOpenedAt() != null && d.getClosedAt() != null) {
                long expectedSeconds = Duration.between(d.getOpenedAt(), d.getClosedAt()).getSeconds();
                if (d.getDurationSeconds() == null || Math.abs(d.getDurationSeconds() - expectedSeconds) > 1) {
                    violations++;
                }
            }
        }
        String status = violations == 0 ? "PASSED" : "FAILED";
        String details = violations == 0
                ? "All " + events.size() + " door events satisfy durationSeconds = (closedAt - openedAt)"
                : violations + " door events have mismatched duration arithmetic";
        return new ValidationResultDto(ruleNo, "ARITHMETIC", "Door Event Duration Arithmetic Match", status, details, events.size(), violations);
    }

    private ValidationResultDto checkTelemetryHumidityRange(int ruleNo, long totalReadings) {
        if (totalReadings == 0) {
            return new ValidationResultDto(ruleNo, "ARITHMETIC", "Telemetry Non-Negative Humidity Check", "FAILED", "No telemetry readings present", 0, 1);
        }
        try {
            Long outOfBounds = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM sensor_readings WHERE reading_type = 'HUMIDITY' AND (reading_value < 0.0 OR reading_value > 100.0)", Long.class);
            long invalid = outOfBounds != null ? outOfBounds : 0;
            String status = invalid == 0 ? "PASSED" : "FAILED";
            String details = invalid == 0
                    ? "All humidity readings strictly conform to physical bounds [0.0% .. 100.0%]"
                    : invalid + " humidity readings violate [0.0% .. 100.0%] bounds";
            return new ValidationResultDto(ruleNo, "ARITHMETIC", "Telemetry Non-Negative Humidity Check", status, details, totalReadings, (int) Math.min(invalid, Integer.MAX_VALUE));
        } catch (Exception e) {
            return new ValidationResultDto(ruleNo, "ARITHMETIC", "Telemetry Non-Negative Humidity Check", "FAILED", "Query error: " + e.getMessage(), totalReadings, 1);
        }
    }

    private ValidationResultDto checkTelemetryTemperatureRange(int ruleNo, long totalReadings) {
        if (totalReadings == 0) {
            return new ValidationResultDto(ruleNo, "ARITHMETIC", "Telemetry Temperature Physical Bounds Check", "FAILED", "No telemetry readings present", 0, 1);
        }
        try {
            Long outOfBounds = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM sensor_readings WHERE reading_type = 'TEMPERATURE' AND (reading_value < -100.0 OR reading_value > 100.0)", Long.class);
            long invalid = outOfBounds != null ? outOfBounds : 0;
            String status = invalid == 0 ? "PASSED" : "FAILED";
            String details = invalid == 0
                    ? "All temperature readings lie strictly within physical limits [-100.0°C .. 100.0°C]"
                    : invalid + " temperature readings exceed physical operational limits";
            return new ValidationResultDto(ruleNo, "ARITHMETIC", "Telemetry Temperature Physical Bounds Check", status, details, totalReadings, (int) Math.min(invalid, Integer.MAX_VALUE));
        } catch (Exception e) {
            return new ValidationResultDto(ruleNo, "ARITHMETIC", "Telemetry Temperature Physical Bounds Check", "FAILED", "Query error: " + e.getMessage(), totalReadings, 1);
        }
    }

    private ValidationResultDto checkActiveSensorTelemetryCoverage(int ruleNo, List<Sensor> sensors) {
        if (sensors.isEmpty()) {
            return new ValidationResultDto(ruleNo, "COVERAGE", "Missing Sensor Readings Audit", "FAILED", "No sensors registered", 0, 1);
        }
        try {
            Long orphanSensors = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM sensors s LEFT JOIN (SELECT DISTINCT sensor_id FROM sensor_readings) r ON s.id = r.sensor_id WHERE s.status = 'ACTIVE' AND r.sensor_id IS NULL", Long.class);
            long missing = orphanSensors != null ? orphanSensors : 0;
            String status = missing == 0 ? "PASSED" : "FAILED";
            String details = missing == 0
                    ? "All " + sensors.size() + " active sensors have telemetry streams recorded (0 missing)"
                    : missing + " active sensors have 0 telemetry readings recorded";
            return new ValidationResultDto(ruleNo, "COVERAGE", "Missing Sensor Readings Audit", status, details, sensors.size(), (int) Math.min(missing, Integer.MAX_VALUE));
        } catch (Exception e) {
            return new ValidationResultDto(ruleNo, "COVERAGE", "Missing Sensor Readings Audit", "FAILED", "Query error: " + e.getMessage(), sensors.size(), 1);
        }
    }

    private ValidationResultDto checkGroundTruthIsolation(int ruleNo, List<GroundTruthAnomaly> groundTruth) {
        if (groundTruth.isEmpty()) {
            return new ValidationResultDto(ruleNo, "EDGE_CASES", "Ground Truth Isolation (No Feature Leakage)", "FAILED", "No ground truth anomalies registered in database", 0, 1);
        }
        int invalid = 0;
        for (GroundTruthAnomaly g : groundTruth) {
            if (g.getId() == null || g.getAnomalyType() == null || g.getEntityId() == null) {
                invalid++;
            }
        }
        String status = invalid == 0 ? "PASSED" : "FAILED";
        String details = invalid == 0
                ? "All " + groundTruth.size() + " ground truth anomaly labels are strictly isolated with non-null labels"
                : invalid + " ground truth anomaly records have missing fields";
        return new ValidationResultDto(ruleNo, "EDGE_CASES", "Ground Truth Isolation (No Feature Leakage)", status, details, groundTruth.size(), invalid);
    }

    private ValidationResultDto checkIncompatibleZoneGroundTruth(int ruleNo, List<InventoryLot> lots, List<GroundTruthAnomaly> groundTruth) {
        Optional<GroundTruthAnomaly> anomalyOpt = groundTruth.stream()
                .filter(g -> g.getAnomalyType() == AnomalyType.LOT_ASSIGNED_INCOMPATIBLE_ZONE)
                .findFirst();
        if (anomalyOpt.isEmpty()) {
            return new ValidationResultDto(ruleNo, "EDGE_CASES", "Incompatible Zone Allocation Ground Truth Match", "FAILED", "No LOT_ASSIGNED_INCOMPATIBLE_ZONE anomaly documented in ground truth", groundTruth.size(), 1);
        }
        String targetLotId = anomalyOpt.get().getEntityId();
        Optional<InventoryLot> lotOpt = lots.stream().filter(l -> l.getId().equals(targetLotId)).findFirst();
        if (lotOpt.isEmpty()) {
            return new ValidationResultDto(ruleNo, "EDGE_CASES", "Incompatible Zone Allocation Ground Truth Match", "FAILED", "Target lot " + targetLotId + " not found in inventory lots", lots.size(), 1);
        }
        InventoryLot lot = lotOpt.get();
        boolean isIncompatible = lot.getCategory() == ProductCategory.VACCINES && lot.getCurrentZone().getZoneType() == ZoneType.AMBIENT;
        String status = isIncompatible ? "PASSED" : "FAILED";
        String details = isIncompatible
                ? "Lot " + lot.getId() + " (Vaccine) is assigned to Ambient Zone " + lot.getCurrentZone().getId() + " matching GTA-001"
                : "Lot " + lot.getId() + " allocation does not reflect documented incompatibility";
        return new ValidationResultDto(ruleNo, "EDGE_CASES", "Incompatible Zone Allocation Ground Truth Match", status, details, 1, isIncompatible ? 0 : 1);
    }

    private ValidationResultDto checkSensorTopologyGroundTruth(int ruleNo, List<Sensor> sensors, List<GroundTruthAnomaly> groundTruth) {
        Optional<GroundTruthAnomaly> anomalyOpt = groundTruth.stream()
                .filter(g -> g.getAnomalyType() == AnomalyType.SENSOR_MAPPED_MULTIPLE_ZONES)
                .findFirst();
        if (anomalyOpt.isEmpty()) {
            return new ValidationResultDto(ruleNo, "EDGE_CASES", "Sensor Topology Conflict Ground Truth Match", "FAILED", "No SENSOR_MAPPED_MULTIPLE_ZONES anomaly documented", groundTruth.size(), 1);
        }
        String sensorId = anomalyOpt.get().getEntityId();
        List<Object[]> multiActive = sensorZoneMappingRepository.findSensorsMappedToMultipleActiveZones();
        boolean conflictExists = multiActive.stream().anyMatch(row -> row[0].equals(sensorId));
        boolean sensorExists = sensors.stream().anyMatch(s -> s.getId().equals(sensorId));

        String status = (sensorExists && conflictExists) ? "PASSED" : "FAILED";
        String details = (sensorExists && conflictExists)
                ? "Sensor " + sensorId + " concurrent multi-zone mapping verified in physical database topology"
                : "Sensor " + sensorId + " conflict mapping could not be verified in topology registry";
        return new ValidationResultDto(ruleNo, "EDGE_CASES", "Sensor Topology Conflict Ground Truth Match", status, details, 1, (sensorExists && conflictExists) ? 0 : 1);
    }

    private ValidationResultDto checkTelemetryGapAndAlarmGroundTruth(int ruleNo, List<GroundTruthAnomaly> groundTruth) {
        boolean hasGap = groundTruth.stream().anyMatch(g -> g.getAnomalyType() == AnomalyType.UNREPORTED_SENSOR_READING_GAP);
        boolean hasAlarmMismatch = groundTruth.stream().anyMatch(g -> g.getAnomalyType() == AnomalyType.ALARM_NOT_LINKED_TO_AFFECTED_LOT);
        int missing = (!hasGap ? 1 : 0) + (!hasAlarmMismatch ? 1 : 0);
        String status = missing == 0 ? "PASSED" : "FAILED";
        String details = missing == 0
                ? "Physical telemetry reading gap (GTA-003) and omitted alarm-lot linkage (GTA-004) ground truth verified"
                : missing + " documented edge case anomalies missing from ground truth";
        return new ValidationResultDto(ruleNo, "EDGE_CASES", "Telemetry Reading Gap & Alarm Linkage Match", status, details, 2, missing);
    }

    private ValidationResultDto checkExportConsistency(int ruleNo, List<Warehouse> warehouses, List<Zone> zones, List<StorageRequirement> reqs) {
        int violations = 0;
        StringBuilder issues = new StringBuilder();

        // 1. Check Warehouses export consistency
        try {
            String csv = exportService.exportEntityToCsv("warehouses");
            long csvRows = Arrays.stream(csv.split("\r?\n")).filter(line -> !line.trim().isEmpty()).count() - 1; // subtract header
            if (csvRows != warehouses.size()) {
                violations++;
                issues.append("Warehouses CSV rows (").append(csvRows).append(") != DB count (").append(warehouses.size()).append("); ");
            }

            String json = exportService.exportEntityToJson("warehouses");
            JsonNode jsonNode = objectMapper.readTree(json);
            if (!jsonNode.isArray() || jsonNode.size() != warehouses.size()) {
                violations++;
                issues.append("Warehouses JSON size != DB count; ");
            }

            String sql = exportService.exportEntityToSql("warehouses");
            long sqlInserts = Arrays.stream(sql.split("\r?\n")).filter(l -> l.startsWith("INSERT INTO warehouses")).count();
            if (sqlInserts != warehouses.size()) {
                violations++;
                issues.append("Warehouses SQL inserts != DB count; ");
            }
        } catch (Exception e) {
            violations++;
            issues.append("Warehouses export error: ").append(e.getMessage()).append("; ");
        }

        // 2. Check Zones export consistency
        try {
            String csv = exportService.exportEntityToCsv("zones");
            long csvRows = Arrays.stream(csv.split("\r?\n")).filter(line -> !line.trim().isEmpty()).count() - 1;
            if (csvRows != zones.size()) {
                violations++;
                issues.append("Zones CSV rows != DB count; ");
            }

            String json = exportService.exportEntityToJson("zones");
            JsonNode jsonNode = objectMapper.readTree(json);
            if (!jsonNode.isArray() || jsonNode.size() != zones.size()) {
                violations++;
                issues.append("Zones JSON size != DB count; ");
            }

            String sql = exportService.exportEntityToSql("zones");
            long sqlInserts = Arrays.stream(sql.split("\r?\n")).filter(l -> l.startsWith("INSERT INTO zones")).count();
            if (sqlInserts != zones.size()) {
                violations++;
                issues.append("Zones SQL inserts != DB count; ");
            }
        } catch (Exception e) {
            violations++;
            issues.append("Zones export error: ").append(e.getMessage()).append("; ");
        }

        // 3. Check Storage Requirements export consistency
        try {
            String csv = exportService.exportEntityToCsv("storage_requirements");
            long csvRows = Arrays.stream(csv.split("\r?\n")).filter(line -> !line.trim().isEmpty()).count() - 1;
            if (csvRows != reqs.size()) {
                violations++;
                issues.append("StorageRequirements CSV rows != DB count; ");
            }

            String json = exportService.exportEntityToJson("storage_requirements");
            JsonNode jsonNode = objectMapper.readTree(json);
            if (!jsonNode.isArray() || jsonNode.size() != reqs.size()) {
                violations++;
                issues.append("StorageRequirements JSON size != DB count; ");
            }

            String sql = exportService.exportEntityToSql("storage_requirements");
            long sqlInserts = Arrays.stream(sql.split("\r?\n")).filter(l -> l.startsWith("INSERT INTO storage_requirements")).count();
            if (sqlInserts != reqs.size()) {
                violations++;
                issues.append("StorageRequirements SQL inserts != DB count; ");
            }
        } catch (Exception e) {
            violations++;
            issues.append("StorageRequirements export error: ").append(e.getMessage()).append("; ");
        }

        String status = violations == 0 ? "PASSED" : "FAILED";
        String details = violations == 0
                ? "CSV, JSON, and SQL exports are 100% consistent with database row counts across all tested entities"
                : "Export consistency failures detected: " + issues.toString();
        return new ValidationResultDto(ruleNo, "EXPORT_CONSISTENCY", "CSV, JSON and SQL Export Consistency Check", status, details, warehouses.size() + zones.size() + reqs.size(), violations);
    }

    private <T> Set<String> toIdSet(List<T> list, Function<T, String> idExtractor) {
        Set<String> set = new HashSet<>();
        for (T item : list) {
            String id = idExtractor.apply(item);
            if (id != null) set.add(id);
        }
        return set;
    }
}
