package com.warehouse.riskmonitor.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.warehouse.riskmonitor.entity.*;
import com.warehouse.riskmonitor.repository.*;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class ExportService {

    private final WarehouseRepository warehouseRepository;
    private final ZoneRepository zoneRepository;
    private final StorageRequirementRepository storageRequirementRepository;
    private final SensorRepository sensorRepository;
    private final SensorZoneMappingRepository sensorZoneMappingRepository;
    private final SensorReadingRepository sensorReadingRepository;
    private final DoorEventRepository doorEventRepository;
    private final InventoryLotRepository inventoryLotRepository;
    private final AlertRepository alertRepository;
    private final InspectionRepository inspectionRepository;
    private final RemediationRepository remediationRepository;
    private final GroundTruthAnomalyRepository groundTruthAnomalyRepository;
    private final AlarmLotImpactRepository alarmLotImpactRepository;
    private final LotZoneAssignmentRepository lotZoneAssignmentRepository;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public ExportService(WarehouseRepository warehouseRepository,
                         ZoneRepository zoneRepository,
                         StorageRequirementRepository storageRequirementRepository,
                         SensorRepository sensorRepository,
                         SensorZoneMappingRepository sensorZoneMappingRepository,
                         SensorReadingRepository sensorReadingRepository,
                         DoorEventRepository doorEventRepository,
                         InventoryLotRepository inventoryLotRepository,
                         AlertRepository alertRepository,
                         InspectionRepository inspectionRepository,
                         RemediationRepository remediationRepository,
                         GroundTruthAnomalyRepository groundTruthAnomalyRepository,
                         AlarmLotImpactRepository alarmLotImpactRepository,
                         LotZoneAssignmentRepository lotZoneAssignmentRepository,
                         JdbcTemplate jdbcTemplate) {
        this.warehouseRepository = warehouseRepository;
        this.zoneRepository = zoneRepository;
        this.storageRequirementRepository = storageRequirementRepository;
        this.sensorRepository = sensorRepository;
        this.sensorZoneMappingRepository = sensorZoneMappingRepository;
        this.sensorReadingRepository = sensorReadingRepository;
        this.doorEventRepository = doorEventRepository;
        this.inventoryLotRepository = inventoryLotRepository;
        this.alertRepository = alertRepository;
        this.inspectionRepository = inspectionRepository;
        this.remediationRepository = remediationRepository;
        this.groundTruthAnomalyRepository = groundTruthAnomalyRepository;
        this.alarmLotImpactRepository = alarmLotImpactRepository;
        this.lotZoneAssignmentRepository = lotZoneAssignmentRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        this.objectMapper.findAndRegisterModules();
    }

    public String exportEntityToCsv(String entityName) {
        StringWriter sw = new StringWriter();
        try {
            switch (entityName.toLowerCase()) {
                case "warehouses" -> {
                    CSVPrinter printer = new CSVPrinter(sw, CSVFormat.DEFAULT.builder().setHeader("id", "name", "location", "capacity_sqm", "total_zones", "status").build());
                    for (Warehouse w : warehouseRepository.findAll()) {
                        printer.printRecord(w.getId(), w.getName(), w.getLocation(), w.getCapacitySqm(), w.getTotalZones(), w.getStatus());
                    }
                    printer.flush();
                }
                case "zones" -> {
                    CSVPrinter printer = new CSVPrinter(sw, CSVFormat.DEFAULT.builder().setHeader("id", "warehouse_id", "name", "zone_type", "capacity_lots", "current_temp", "current_hum", "status").build());
                    for (Zone z : zoneRepository.findAll()) {
                        printer.printRecord(z.getId(), z.getWarehouse().getId(), z.getName(), z.getZoneType(), z.getCapacityLots(), z.getCurrentTemperature(), z.getCurrentHumidity(), z.getStatus());
                    }
                    printer.flush();
                }
                case "sensors" -> {
                    CSVPrinter printer = new CSVPrinter(sw, CSVFormat.DEFAULT.builder().setHeader("id", "zone_id", "sensor_type", "status", "model_number", "calibration_offset").build());
                    for (Sensor s : sensorRepository.findAll()) {
                        printer.printRecord(s.getId(), s.getZone().getId(), s.getSensorType(), s.getStatus(), s.getModelNumber(), s.getCalibrationOffset());
                    }
                    printer.flush();
                }
                case "sensor_zone_mappings" -> {
                    CSVPrinter printer = new CSVPrinter(sw, CSVFormat.DEFAULT.builder().setHeader("id", "sensor_id", "zone_id", "mapped_at", "unmapped_at", "is_active", "rationale").build());
                    for (SensorZoneMapping m : sensorZoneMappingRepository.findAll()) {
                        printer.printRecord(m.getId(), m.getSensor().getId(), m.getZone().getId(), m.getMappedAt(), m.getUnmappedAt(), m.getIsActive(), m.getRationale());
                    }
                    printer.flush();
                }
                case "lots", "inventory_lots" -> {
                    CSVPrinter printer = new CSVPrinter(sw, CSVFormat.DEFAULT.builder().setHeader("id", "product_name", "sku", "category", "quantity", "unit", "storage_req_id", "zone_id", "status", "thermal_stress").build());
                    for (InventoryLot l : inventoryLotRepository.findAll()) {
                        printer.printRecord(l.getId(), l.getProductName(), l.getSku(), l.getCategory(), l.getQuantity(), l.getUnit(), l.getStorageRequirement().getId(), l.getCurrentZone().getId(), l.getStatus(), l.getThermalStressScore());
                    }
                    printer.flush();
                }
                case "lot_zone_assignments" -> {
                    CSVPrinter printer = new CSVPrinter(sw, CSVFormat.DEFAULT.builder().setHeader("id", "lot_id", "zone_id", "ingress_timestamp", "egress_timestamp", "dwell_minutes").build());
                    for (LotZoneAssignment a : lotZoneAssignmentRepository.findAll()) {
                        printer.printRecord(a.getId(), a.getLot().getId(), a.getZone().getId(), a.getIngressTimestamp(), a.getEgressTimestamp(), a.getDwellMinutes());
                    }
                    printer.flush();
                }
                case "alerts" -> {
                    CSVPrinter printer = new CSVPrinter(sw, CSVFormat.DEFAULT.builder().setHeader("id", "zone_id", "alert_type", "risk_level", "status", "triggered_at", "trigger_value", "threshold_value", "message").build());
                    for (Alert a : alertRepository.findAll()) {
                        printer.printRecord(a.getId(), a.getZone().getId(), a.getAlertType(), a.getRiskLevel(), a.getStatus(), a.getTriggeredAt(), a.getTriggerValue(), a.getThresholdValue(), a.getMessage());
                    }
                    printer.flush();
                }
                case "alarm_lot_impacts" -> {
                    CSVPrinter printer = new CSVPrinter(sw, CSVFormat.DEFAULT.builder().setHeader("id", "alert_id", "lot_id", "exposure_minutes", "thermal_stress_score", "action_recommended").build());
                    for (AlarmLotImpact i : alarmLotImpactRepository.findAll()) {
                        printer.printRecord(i.getId(), i.getAlert().getId(), i.getLot().getId(), i.getExposureMinutes(), i.getThermalStressScore(), i.getActionRecommended());
                    }
                    printer.flush();
                }
                case "door_events", "doors" -> {
                    CSVPrinter printer = new CSVPrinter(sw, CSVFormat.DEFAULT.builder().setHeader("id", "zone_id", "door_name", "opened_at", "closed_at", "duration_seconds", "max_permitted_seconds", "status", "alert_triggered").build());
                    for (DoorEvent d : doorEventRepository.findAll()) {
                        printer.printRecord(d.getId(), d.getZone().getId(), d.getDoorName(), d.getOpenedAt(), d.getClosedAt(), d.getDurationSeconds(), d.getMaxPermittedSeconds(), d.getStatus(), d.getAlertTriggered());
                    }
                    printer.flush();
                }
                case "storage_requirements" -> {
                    CSVPrinter printer = new CSVPrinter(sw, CSVFormat.DEFAULT.builder().setHeader("id", "name", "category", "temp_target", "temp_min", "temp_max", "humidity_target", "humidity_min", "humidity_max", "max_door_open_seconds").build());
                    for (StorageRequirement r : storageRequirementRepository.findAll()) {
                        printer.printRecord(r.getId(), r.getName(), r.getCategory(), r.getTempTarget(), r.getTempMin(), r.getTempMax(), r.getHumidityTarget(), r.getHumidityMin(), r.getHumidityMax(), r.getMaxDoorOpenSeconds());
                    }
                    printer.flush();
                }
                case "inspections" -> {
                    CSVPrinter printer = new CSVPrinter(sw, CSVFormat.DEFAULT.builder().setHeader("id", "zone_id", "alert_id", "inspector_name", "inspection_date", "status", "observations", "findings_summary", "action_required").build());
                    for (Inspection i : inspectionRepository.findAll()) {
                        printer.printRecord(i.getId(), i.getZone().getId(), i.getAlert() != null ? i.getAlert().getId() : "", i.getInspectorName(), i.getInspectionDate(), i.getStatus(), i.getObservations(), i.getFindingsSummary(), i.getActionRequired());
                    }
                    printer.flush();
                }
                case "remediations" -> {
                    CSVPrinter printer = new CSVPrinter(sw, CSVFormat.DEFAULT.builder().setHeader("id", "alert_id", "inspection_id", "action_title", "action_type", "assigned_to", "cost_impact", "scheduled_at", "completed_at", "status", "resolution_notes").build());
                    for (Remediation r : remediationRepository.findAll()) {
                        printer.printRecord(r.getId(), r.getAlert().getId(), r.getInspection() != null ? r.getInspection().getId() : "", r.getActionTitle(), r.getActionType(), r.getAssignedTo(), r.getCostImpact(), r.getScheduledAt(), r.getCompletedAt(), r.getStatus(), r.getResolutionNotes());
                    }
                    printer.flush();
                }
                case "ground_truth", "ground_truth_anomalies" -> {
                    CSVPrinter printer = new CSVPrinter(sw, CSVFormat.DEFAULT.builder().setHeader("id", "entity_type", "entity_id", "anomaly_type", "description", "expected_action", "detected").build());
                    for (GroundTruthAnomaly g : groundTruthAnomalyRepository.findAll()) {
                        printer.printRecord(g.getId(), g.getEntityType(), g.getEntityId(), g.getAnomalyType(), g.getDescription(), g.getExpectedAction(), g.getDetected());
                    }
                    printer.flush();
                }
                case "sensor_readings", "readings" -> {
                    CSVPrinter printer = new CSVPrinter(sw, CSVFormat.DEFAULT.builder().setHeader("id", "sensor_id", "zone_id", "reading_type", "reading_value", "unit", "timestamp", "status", "is_anomaly").build());
                    jdbcTemplate.query("SELECT id, sensor_id, zone_id, reading_type, reading_value, unit, timestamp, status, is_anomaly FROM sensor_readings ORDER BY timestamp ASC, id ASC", rs -> {
                        try {
                            printer.printRecord(
                                rs.getString(1), rs.getString(2), rs.getString(3),
                                rs.getString(4), rs.getDouble(5), rs.getString(6),
                                rs.getString(7), rs.getString(8), rs.getBoolean(9)
                            );
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    });
                    printer.flush();
                }
                default -> throw new IllegalArgumentException("Unknown entity for CSV export: " + entityName);
            }
        } catch (Exception e) {
            throw new RuntimeException("CSV Export failed: " + e.getMessage(), e);
        }
        return sw.toString();
    }

    public String exportEntityToJson(String entityName) {
        try {
            if ("sensor_readings".equalsIgnoreCase(entityName) || "readings".equalsIgnoreCase(entityName)) {
                StringBuilder jsonSb = new StringBuilder();
                jsonSb.append("[\n");
                boolean[] first = {true};
                jdbcTemplate.query("SELECT id, sensor_id, zone_id, reading_type, reading_value, unit, timestamp, status, is_anomaly FROM sensor_readings ORDER BY timestamp ASC, id ASC", rs -> {
                    if (!first[0]) {
                        jsonSb.append(",\n");
                    }
                    first[0] = false;
                    jsonSb.append(String.format("  {\"id\":\"%s\",\"sensorId\":\"%s\",\"zoneId\":\"%s\",\"readingType\":\"%s\",\"value\":%.2f,\"unit\":\"%s\",\"timestamp\":\"%s\",\"status\":\"%s\",\"isAnomaly\":%b}",
                        rs.getString(1), rs.getString(2), rs.getString(3),
                        rs.getString(4), rs.getDouble(5), rs.getString(6),
                        rs.getString(7), rs.getString(8), rs.getBoolean(9)));
                });
                jsonSb.append("\n]");
                return jsonSb.toString();
            }

            Object data = switch (entityName.toLowerCase()) {
                case "warehouses" -> warehouseRepository.findAll();
                case "zones" -> zoneRepository.findAll();
                case "sensors" -> sensorRepository.findAll();
                case "sensor_zone_mappings" -> sensorZoneMappingRepository.findAll();
                case "lots", "inventory_lots" -> inventoryLotRepository.findAll();
                case "lot_zone_assignments" -> lotZoneAssignmentRepository.findAll();
                case "alerts" -> alertRepository.findAll();
                case "alarm_lot_impacts" -> alarmLotImpactRepository.findAll();
                case "door_events", "doors" -> doorEventRepository.findAll();
                case "storage_requirements" -> storageRequirementRepository.findAll();
                case "inspections" -> inspectionRepository.findAll();
                case "remediations" -> remediationRepository.findAll();
                case "ground_truth", "ground_truth_anomalies" -> groundTruthAnomalyRepository.findAll();
                default -> throw new IllegalArgumentException("Unknown entity for JSON export: " + entityName);
            };
            return objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            throw new RuntimeException("JSON Export failed: " + e.getMessage(), e);
        }
    }

    public String exportEntityToSql(String entityName) {
        StringBuilder sb = new StringBuilder();
        sb.append("-- YottaFlex Warehouse Monitoring System SQL Seed Dump\n");
        sb.append("-- Entity: ").append(entityName).append("\n\n");

        switch (entityName.toLowerCase()) {
            case "warehouses" -> {
                for (Warehouse w : warehouseRepository.findAll()) {
                    sb.append(String.format("INSERT INTO warehouses (id, name, location, capacity_sqm, total_zones, status) VALUES ('%s', '%s', '%s', %d, %d, '%s');\n",
                            w.getId(), escapeSql(w.getName()), escapeSql(w.getLocation()), w.getCapacitySqm(), w.getTotalZones(), w.getStatus().name()));
                }
            }
            case "zones" -> {
                for (Zone z : zoneRepository.findAll()) {
                    sb.append(String.format("INSERT INTO zones (id, warehouse_id, name, zone_type, capacity_lots, current_temperature, current_humidity, status) VALUES ('%s', '%s', '%s', '%s', %d, %.2f, %.2f, '%s');\n",
                            z.getId(), z.getWarehouse().getId(), escapeSql(z.getName()), z.getZoneType().name(), z.getCapacityLots(),
                            z.getCurrentTemperature() != null ? z.getCurrentTemperature() : 0.0,
                            z.getCurrentHumidity() != null ? z.getCurrentHumidity() : 0.0,
                            z.getStatus().name()));
                }
            }
            case "storage_requirements" -> {
                for (StorageRequirement r : storageRequirementRepository.findAll()) {
                    sb.append(String.format("INSERT INTO storage_requirements (id, name, category, temp_target, temp_min, temp_max, humidity_target, humidity_min, humidity_max, max_door_open_seconds) VALUES ('%s', '%s', '%s', %.1f, %.1f, %.1f, %.1f, %.1f, %.1f, %d);\n",
                            r.getId(), escapeSql(r.getName()), r.getCategory().name(), r.getTempTarget(), r.getTempMin(), r.getTempMax(), r.getHumidityTarget(), r.getHumidityMin(), r.getHumidityMax(), r.getMaxDoorOpenSeconds()));
                }
            }
            case "sensors" -> {
                for (Sensor s : sensorRepository.findAll()) {
                    sb.append(String.format("INSERT INTO sensors (id, zone_id, sensor_type, status, model_number, calibration_offset) VALUES ('%s', '%s', '%s', '%s', '%s', %.2f);\n",
                            s.getId(), s.getZone().getId(), s.getSensorType().name(), s.getStatus().name(), escapeSql(s.getModelNumber()), s.getCalibrationOffset()));
                }
            }
            case "sensor_zone_mappings" -> {
                for (SensorZoneMapping m : sensorZoneMappingRepository.findAll()) {
                    sb.append(String.format("INSERT INTO sensor_zone_mappings (id, sensor_id, zone_id, mapped_at, is_active, rationale) VALUES ('%s', '%s', '%s', '%s', %d, '%s');\n",
                            m.getId(), m.getSensor().getId(), m.getZone().getId(), m.getMappedAt(), m.getIsActive() ? 1 : 0, escapeSql(m.getRationale())));
                }
            }
            case "alerts" -> {
                for (Alert a : alertRepository.findAll()) {
                    sb.append(String.format("INSERT INTO alerts (id, zone_id, alert_type, risk_level, status, message) VALUES ('%s', '%s', '%s', '%s', '%s', '%s');\n",
                            a.getId(), a.getZone().getId(), a.getAlertType().name(), a.getRiskLevel().name(), a.getStatus().name(), escapeSql(a.getMessage())));
                }
            }
            case "door_events", "doors" -> {
                for (DoorEvent d : doorEventRepository.findAll()) {
                    sb.append(String.format("INSERT INTO door_events (id, zone_id, door_name, duration_seconds, max_permitted_seconds, status, alert_triggered) VALUES ('%s', '%s', '%s', %d, %d, '%s', %d);\n",
                            d.getId(), d.getZone().getId(), escapeSql(d.getDoorName()), d.getDurationSeconds() != null ? d.getDurationSeconds() : 0, d.getMaxPermittedSeconds(), d.getStatus().name(), d.getAlertTriggered() ? 1 : 0));
                }
            }
            case "lots", "inventory_lots" -> {
                for (InventoryLot l : inventoryLotRepository.findAll()) {
                    sb.append(String.format("INSERT INTO inventory_lots (id, product_name, sku, category, quantity, unit, storage_requirement_id, current_zone_id, status) VALUES ('%s', '%s', '%s', '%s', %.2f, '%s', '%s', '%s', '%s');\n",
                            l.getId(), escapeSql(l.getProductName()), escapeSql(l.getSku()), l.getCategory().name(), l.getQuantity(), l.getUnit().name(), l.getStorageRequirement().getId(), l.getCurrentZone().getId(), l.getStatus().name()));
                }
            }
            case "lot_zone_assignments" -> {
                for (LotZoneAssignment lza : lotZoneAssignmentRepository.findAll()) {
                    sb.append(String.format("INSERT INTO lot_zone_assignments (id, lot_id, zone_id, ingress_timestamp, dwell_minutes) VALUES ('%s', '%s', '%s', '%s', %d);\n",
                            lza.getId(), lza.getLot().getId(), lza.getZone().getId(), lza.getIngressTimestamp(), lza.getDwellMinutes() != null ? lza.getDwellMinutes() : 0));
                }
            }
            case "alarm_lot_impacts" -> {
                for (AlarmLotImpact ali : alarmLotImpactRepository.findAll()) {
                    sb.append(String.format("INSERT INTO alarm_lot_impacts (id, alert_id, lot_id, exposure_minutes, thermal_stress_score, action_recommended) VALUES ('%s', '%s', '%s', %.1f, %.1f, '%s');\n",
                            ali.getId(), ali.getAlert().getId(), ali.getLot().getId(), ali.getExposureMinutes(), ali.getThermalStressScore(), escapeSql(ali.getActionRecommended())));
                }
            }
            case "inspections" -> {
                for (Inspection i : inspectionRepository.findAll()) {
                    sb.append(String.format("INSERT INTO inspections (id, zone_id, alert_id, inspector_name, inspection_date, status, observations, findings_summary, action_required) VALUES ('%s', '%s', %s, '%s', '%s', '%s', '%s', '%s', %d);\n",
                            i.getId(), i.getZone().getId(),
                            i.getAlert() != null ? "'" + i.getAlert().getId() + "'" : "NULL",
                            escapeSql(i.getInspectorName()), i.getInspectionDate(),
                            i.getStatus().name(), escapeSql(i.getObservations()), escapeSql(i.getFindingsSummary()),
                            (i.getActionRequired() != null && i.getActionRequired()) ? 1 : 0));
                }
            }
            case "remediations" -> {
                for (Remediation r : remediationRepository.findAll()) {
                    sb.append(String.format("INSERT INTO remediations (id, alert_id, inspection_id, lot_id, action_title, action_type, assigned_to, status, cost_impact, scheduled_at, completed_at, resolution_notes) VALUES ('%s', '%s', %s, %s, '%s', '%s', '%s', '%s', %.2f, '%s', %s, '%s');\n",
                            r.getId(), r.getAlert().getId(),
                            r.getInspection() != null ? "'" + r.getInspection().getId() + "'" : "NULL",
                            r.getInventoryLot() != null ? "'" + r.getInventoryLot().getId() + "'" : "NULL",
                            escapeSql(r.getActionTitle()), r.getActionType().name(), escapeSql(r.getAssignedTo()),
                            r.getStatus().name(), r.getCostImpact(), r.getScheduledAt(),
                            r.getCompletedAt() != null ? "'" + r.getCompletedAt() + "'" : "NULL",
                            escapeSql(r.getResolutionNotes())));
                }
            }
            case "ground_truth", "ground_truth_anomalies" -> {
                for (GroundTruthAnomaly g : groundTruthAnomalyRepository.findAll()) {
                    sb.append(String.format("INSERT INTO ground_truth_anomalies (id, entity_type, entity_id, anomaly_type, description, expected_action, detected) VALUES ('%s', '%s', '%s', '%s', '%s', '%s', %d);\n",
                            g.getId(), g.getEntityType(), g.getEntityId(), g.getAnomalyType(),
                            escapeSql(g.getDescription()), escapeSql(g.getExpectedAction()), g.getDetected() ? 1 : 0));
                }
            }
            case "sensor_readings", "readings" -> {
                jdbcTemplate.query("SELECT id, sensor_id, zone_id, reading_type, reading_value, unit, timestamp, status, is_anomaly FROM sensor_readings ORDER BY timestamp ASC, id ASC", rs -> {
                    sb.append(String.format("INSERT INTO sensor_readings (id, sensor_id, zone_id, reading_type, reading_value, unit, timestamp, status, is_anomaly) VALUES ('%s', '%s', '%s', '%s', %.2f, '%s', '%s', '%s', %d);\n",
                            rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getDouble(5), rs.getString(6), rs.getString(7), rs.getString(8), rs.getBoolean(9) ? 1 : 0));
                });
            }
            default -> throw new IllegalArgumentException("Unknown entity for SQL export: " + entityName);
        }
        return sb.toString();
    }

    private String escapeSql(String s) {
        if (s == null) return "";
        return s.replace("'", "''");
    }

    public void exportAllToDataDirectory() {
        try {
            File dir = new File("data");
            File csvDir = new File(dir, "csv");
            File jsonDir = new File(dir, "json");
            File sqlDir = new File(dir, "sql");
            csvDir.mkdirs();
            jsonDir.mkdirs();
            sqlDir.mkdirs();

            String[] entities = {
                "warehouses", "zones", "sensors", "sensor_zone_mappings",
                "storage_requirements", "inventory_lots", "lot_zone_assignments",
                "door_events", "alerts", "alarm_lot_impacts", "inspections",
                "remediations", "ground_truth"
            };

            for (String ent : entities) {
                try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(csvDir, ent + ".csv")), StandardCharsets.UTF_8), 32768)) {
                    bw.write(exportEntityToCsv(ent));
                }
                try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(jsonDir, ent + ".json")), StandardCharsets.UTF_8), 32768)) {
                    bw.write(exportEntityToJson(ent));
                }
            }

            // Stream 1,000,000 sensor readings to sensor_readings.csv via high-performance UTF-8 streaming
            File readingsCsv = new File(csvDir, "sensor_readings.csv");
            try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(readingsCsv), StandardCharsets.UTF_8), 65536);
                 CSVPrinter printer = new CSVPrinter(bw, CSVFormat.DEFAULT.builder().setHeader("id", "sensor_id", "zone_id", "reading_type", "reading_value", "unit", "timestamp", "status", "is_anomaly").build())) {
                jdbcTemplate.query("SELECT id, sensor_id, zone_id, reading_type, reading_value, unit, timestamp, status, is_anomaly FROM sensor_readings ORDER BY timestamp ASC, id ASC", rs -> {
                    try {
                        printer.printRecord(
                            rs.getString(1), rs.getString(2), rs.getString(3),
                            rs.getString(4), rs.getDouble(5), rs.getString(6),
                            rs.getString(7), rs.getString(8), rs.getBoolean(9)
                        );
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
                printer.flush();
            }

            // Stream 1,000,000 sensor readings to sensor_readings.json via streaming UTF-8 BufferedWriter
            File readingsJson = new File(jsonDir, "sensor_readings.json");
            try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(readingsJson), StandardCharsets.UTF_8), 65536)) {
                bw.write("[\n");
                boolean[] first = {true};
                jdbcTemplate.query("SELECT id, sensor_id, zone_id, reading_type, reading_value, unit, timestamp, status, is_anomaly FROM sensor_readings ORDER BY timestamp ASC, id ASC", rs -> {
                    try {
                        if (!first[0]) {
                            bw.write(",\n");
                        }
                        first[0] = false;
                        bw.write(String.format("  {\"id\":\"%s\",\"sensorId\":\"%s\",\"zoneId\":\"%s\",\"readingType\":\"%s\",\"value\":%.2f,\"unit\":\"%s\",\"timestamp\":\"%s\",\"status\":\"%s\",\"isAnomaly\":%b}",
                            rs.getString(1), rs.getString(2), rs.getString(3),
                            rs.getString(4), rs.getDouble(5), rs.getString(6),
                            rs.getString(7), rs.getString(8), rs.getBoolean(9)));
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
                bw.write("\n]");
                bw.flush();
            }

            // Stream sensor_readings.sql via streaming UTF-8 BufferedWriter
            File readingsSql = new File(sqlDir, "sensor_readings.sql");
            try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(readingsSql), StandardCharsets.UTF_8), 65536)) {
                bw.write("-- YottaFlex Sensor Readings 1,000,000 Telemetry Stream SQL Dump\n");
                jdbcTemplate.query("SELECT id, sensor_id, zone_id, reading_type, reading_value, unit, timestamp, status, is_anomaly FROM sensor_readings ORDER BY timestamp ASC, id ASC", rs -> {
                    try {
                        bw.write(String.format("INSERT INTO sensor_readings (id, sensor_id, zone_id, reading_type, reading_value, unit, timestamp, status, is_anomaly) VALUES ('%s', '%s', '%s', '%s', %.2f, '%s', '%s', '%s', %d);\n",
                            rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getDouble(5), rs.getString(6), rs.getString(7), rs.getString(8), rs.getBoolean(9) ? 1 : 0));
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
                bw.flush();
            }

            // Write ground_truth to root of data folder as required by challenge
            try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(dir, "ground_truth.csv")), StandardCharsets.UTF_8), 32768)) {
                bw.write(exportEntityToCsv("ground_truth"));
            }
            try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(dir, "ground_truth.json")), StandardCharsets.UTF_8), 32768)) {
                bw.write(exportEntityToJson("ground_truth"));
            }

            // Write seed SQL with all relational master, transaction, and event tables
            try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(sqlDir, "seed.sql")), StandardCharsets.UTF_8), 65536)) {
                bw.write(exportEntityToSql("warehouses"));
                bw.write(exportEntityToSql("zones"));
                bw.write(exportEntityToSql("storage_requirements"));
                bw.write(exportEntityToSql("sensors"));
                bw.write(exportEntityToSql("sensor_zone_mappings"));
                bw.write(exportEntityToSql("inventory_lots"));
                bw.write(exportEntityToSql("lot_zone_assignments"));
                bw.write(exportEntityToSql("door_events"));
                bw.write(exportEntityToSql("alerts"));
                bw.write(exportEntityToSql("alarm_lot_impacts"));
                bw.write(exportEntityToSql("inspections"));
                bw.write(exportEntityToSql("remediations"));
                bw.write(exportEntityToSql("ground_truth"));
            }
        } catch (Exception e) {
            System.err.println("File export error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
