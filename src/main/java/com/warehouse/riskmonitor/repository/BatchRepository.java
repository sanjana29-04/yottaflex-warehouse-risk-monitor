package com.warehouse.riskmonitor.repository;

import com.warehouse.riskmonitor.entity.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Repository
public class BatchRepository {

    private final JdbcTemplate jdbcTemplate;

    public BatchRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public void clearAllTables() {
        try {
            jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 0;");
        } catch (Exception ignored) {}

        String[] tables = {
            "ground_truth_anomalies", "alarm_lot_impacts", "remediations",
            "inspections", "alerts", "lot_zone_assignments", "inventory_lots",
            "door_events", "sensor_readings", "sensor_zone_mappings", "sensors", "zones",
            "storage_requirements", "warehouses"
        };
        for (String table : tables) {
            try {
                jdbcTemplate.execute("TRUNCATE TABLE " + table);
            } catch (Exception e) {
                try {
                    jdbcTemplate.execute("DELETE FROM " + table);
                } catch (Exception ignored) {}
            }
        }

        try {
            jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 1;");
        } catch (Exception ignored) {}
    }

    @Transactional
    public void batchInsertWarehouses(List<Warehouse> list) {
        String sql = "INSERT INTO warehouses (id, name, location, capacity_sqm, total_zones, status, created_at, updated_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        List<Object[]> batch = new ArrayList<>();
        for (Warehouse w : list) {
            batch.add(new Object[]{
                w.getId(), w.getName(), w.getLocation(), w.getCapacitySqm(), w.getTotalZones(),
                w.getStatus().name(), Timestamp.valueOf(w.getCreatedAt()), Timestamp.valueOf(w.getUpdatedAt())
            });
        }
        jdbcTemplate.batchUpdate(sql, batch);
    }

    @Transactional
    public void batchInsertZones(List<Zone> list) {
        String sql = "INSERT INTO zones (id, warehouse_id, name, zone_type, capacity_lots, current_temperature, " +
                     "current_humidity, current_occupancy, max_occupancy, temperature_min, temperature_max, " +
                     "humidity_min, humidity_max, status, created_at, updated_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        List<Object[]> batch = new ArrayList<>();
        for (Zone z : list) {
            batch.add(new Object[]{
                z.getId(), z.getWarehouse().getId(), z.getName(), z.getZoneType().name(), z.getCapacityLots(),
                z.getCurrentTemperature(), z.getCurrentHumidity(), z.getCurrentOccupancy(), z.getMaxOccupancy(),
                z.getTemperatureMin(), z.getTemperatureMax(), z.getHumidityMin(), z.getHumidityMax(),
                z.getStatus().name(), Timestamp.valueOf(z.getCreatedAt()), Timestamp.valueOf(z.getUpdatedAt())
            });
        }
        jdbcTemplate.batchUpdate(sql, batch);
    }

    @Transactional
    public void batchInsertStorageRequirements(List<StorageRequirement> list) {
        String sql = "INSERT INTO storage_requirements (id, name, category, temp_target, temp_min, temp_max, " +
                     "humidity_target, humidity_min, humidity_max, max_door_open_seconds) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        List<Object[]> batch = new ArrayList<>();
        for (StorageRequirement r : list) {
            batch.add(new Object[]{
                r.getId(), r.getName(), r.getCategory().name(), r.getTempTarget(), r.getTempMin(), r.getTempMax(),
                r.getHumidityTarget(), r.getHumidityMin(), r.getHumidityMax(), r.getMaxDoorOpenSeconds()
            });
        }
        jdbcTemplate.batchUpdate(sql, batch);
    }

    @Transactional
    public void batchInsertSensors(List<Sensor> list) {
        String sql = "INSERT INTO sensors (id, zone_id, sensor_type, status, model_number, installation_date, " +
                     "last_calibration_date, calibration_offset, latest_reading_value, latest_reading_time) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        List<Object[]> batch = new ArrayList<>();
        for (Sensor s : list) {
            batch.add(new Object[]{
                s.getId(), s.getZone().getId(), s.getSensorType().name(), s.getStatus().name(), s.getModelNumber(),
                Timestamp.valueOf(s.getInstallationDate()),
                s.getLastCalibrationDate() != null ? Timestamp.valueOf(s.getLastCalibrationDate()) : null,
                s.getCalibrationOffset(), s.getLatestReadingValue(),
                s.getLatestReadingTime() != null ? Timestamp.valueOf(s.getLatestReadingTime()) : null
            });
        }
        jdbcTemplate.batchUpdate(sql, batch);
    }

    @Transactional
    public void batchInsertSensorZoneMappings(List<SensorZoneMapping> list) {
        String sql = "INSERT INTO sensor_zone_mappings (id, sensor_id, zone_id, mapped_at, unmapped_at, is_active, rationale) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";
        List<Object[]> batch = new ArrayList<>();
        for (SensorZoneMapping m : list) {
            batch.add(new Object[]{
                m.getId(), m.getSensor().getId(), m.getZone().getId(),
                Timestamp.valueOf(m.getMappedAt()),
                m.getUnmappedAt() != null ? Timestamp.valueOf(m.getUnmappedAt()) : null,
                m.getIsActive() ? 1 : 0,
                m.getRationale()
            });
        }
        jdbcTemplate.batchUpdate(sql, batch);
    }

    @Transactional
    public void batchInsertSensorReadingsChunk(List<SensorReading> list) {
        String sql = "INSERT INTO sensor_readings (id, sensor_id, zone_id, reading_type, reading_value, unit, " +
                     "timestamp, status, is_anomaly) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        jdbcTemplate.batchUpdate(sql, new org.springframework.jdbc.core.BatchPreparedStatementSetter() {
            @Override
            public void setValues(java.sql.PreparedStatement ps, int i) throws java.sql.SQLException {
                SensorReading r = list.get(i);
                ps.setString(1, r.getId());
                ps.setString(2, r.getSensor().getId());
                ps.setString(3, r.getZone().getId());
                ps.setString(4, r.getReadingType().name());
                ps.setDouble(5, r.getValue());
                ps.setString(6, r.getUnit());
                ps.setTimestamp(7, Timestamp.valueOf(r.getTimestamp()));
                ps.setString(8, r.getStatus().name());
                ps.setInt(9, r.getIsAnomaly() ? 1 : 0);
            }

            @Override
            public int getBatchSize() {
                return list.size();
            }
        });
    }

    @Transactional
    public void batchInsertDoorEvents(List<DoorEvent> list) {
        String sql = "INSERT INTO door_events (id, zone_id, door_name, opened_at, closed_at, duration_seconds, " +
                     "max_permitted_seconds, status, alert_triggered) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        List<Object[]> batch = new ArrayList<>();
        for (DoorEvent d : list) {
            batch.add(new Object[]{
                d.getId(), d.getZone().getId(), d.getDoorName(),
                Timestamp.valueOf(d.getOpenedAt()),
                d.getClosedAt() != null ? Timestamp.valueOf(d.getClosedAt()) : null,
                d.getDurationSeconds(), d.getMaxPermittedSeconds(),
                d.getStatus().name(), d.getAlertTriggered() ? 1 : 0
            });
        }
        jdbcTemplate.batchUpdate(sql, batch);
    }

    @Transactional
    public void batchInsertInventoryLotsChunk(List<InventoryLot> list) {
        String sql = "INSERT INTO inventory_lots (id, product_name, sku, category, quantity, unit, " +
                     "storage_requirement_id, current_zone_id, ingress_timestamp, expiry_date, status, " +
                     "exposure_duration_minutes, thermal_stress_score) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        List<Object[]> batch = new ArrayList<>();
        for (InventoryLot l : list) {
            batch.add(new Object[]{
                l.getId(), l.getProductName(), l.getSku(), l.getCategory().name(), l.getQuantity(),
                l.getUnit().name(), l.getStorageRequirement().getId(), l.getCurrentZone().getId(),
                Timestamp.valueOf(l.getIngressTimestamp()), Timestamp.valueOf(l.getExpiryDate()),
                l.getStatus().name(), l.getExposureDurationMinutes(), l.getThermalStressScore()
            });
        }
        jdbcTemplate.batchUpdate(sql, batch);
    }

    @Transactional
    public void batchInsertLotZoneAssignments(List<LotZoneAssignment> list) {
        String sql = "INSERT INTO lot_zone_assignments (id, lot_id, zone_id, ingress_timestamp, egress_timestamp, dwell_minutes) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";
        List<Object[]> batch = new ArrayList<>();
        for (LotZoneAssignment lza : list) {
            batch.add(new Object[]{
                lza.getId(), lza.getLot().getId(), lza.getZone().getId(),
                Timestamp.valueOf(lza.getIngressTimestamp()),
                lza.getEgressTimestamp() != null ? Timestamp.valueOf(lza.getEgressTimestamp()) : null,
                lza.getDwellMinutes()
            });
        }
        jdbcTemplate.batchUpdate(sql, batch);
    }

    @Transactional
    public void batchInsertAlerts(List<Alert> list) {
        String sql = "INSERT INTO alerts (id, zone_id, sensor_id, alert_type, risk_level, status, " +
                     "triggered_at, resolved_at, trigger_value, threshold_value, message, acknowledged_by, resolved_by, notes) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        List<Object[]> batch = new ArrayList<>();
        for (Alert a : list) {
            batch.add(new Object[]{
                a.getId(), a.getZone().getId(), a.getSensor() != null ? a.getSensor().getId() : null,
                a.getAlertType().name(), a.getRiskLevel().name(), a.getStatus().name(),
                Timestamp.valueOf(a.getTriggeredAt()),
                a.getResolvedAt() != null ? Timestamp.valueOf(a.getResolvedAt()) : null,
                a.getTriggerValue(), a.getThresholdValue(), a.getMessage(),
                a.getAcknowledgedBy(), a.getResolvedBy(), a.getNotes()
            });
        }
        jdbcTemplate.batchUpdate(sql, batch);
    }

    @Transactional
    public void batchInsertAlarmLotImpacts(List<AlarmLotImpact> list) {
        String sql = "INSERT INTO alarm_lot_impacts (id, alert_id, lot_id, exposure_minutes, thermal_stress_score, action_recommended) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";
        List<Object[]> batch = new ArrayList<>();
        for (AlarmLotImpact ali : list) {
            batch.add(new Object[]{
                ali.getId(), ali.getAlert().getId(), ali.getLot().getId(),
                ali.getExposureMinutes(), ali.getThermalStressScore(), ali.getActionRecommended()
            });
        }
        jdbcTemplate.batchUpdate(sql, batch);
    }

    @Transactional
    public void batchInsertInspections(List<Inspection> list) {
        String sql = "INSERT INTO inspections (id, zone_id, alert_id, inspector_name, inspection_date, status, " +
                     "observations, findings_summary, action_required) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        List<Object[]> batch = new ArrayList<>();
        for (Inspection i : list) {
            batch.add(new Object[]{
                i.getId(), i.getZone().getId(), i.getAlert() != null ? i.getAlert().getId() : null,
                i.getInspectorName(), Timestamp.valueOf(i.getInspectionDate()), i.getStatus().name(),
                i.getObservations(), i.getFindingsSummary(), i.getActionRequired() ? 1 : 0
            });
        }
        jdbcTemplate.batchUpdate(sql, batch);
    }

    @Transactional
    public void batchInsertRemediations(List<Remediation> list) {
        String sql = "INSERT INTO remediations (id, alert_id, inspection_id, lot_id, action_title, action_type, " +
                     "assigned_to, status, cost_impact, scheduled_at, completed_at, resolution_notes) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        List<Object[]> batch = new ArrayList<>();
        for (Remediation r : list) {
            batch.add(new Object[]{
                r.getId(), r.getAlert().getId(),
                r.getInspection() != null ? r.getInspection().getId() : null,
                r.getInventoryLot() != null ? r.getInventoryLot().getId() : null,
                r.getActionTitle(), r.getActionType().name(), r.getAssignedTo(), r.getStatus().name(),
                r.getCostImpact(), Timestamp.valueOf(r.getScheduledAt()),
                r.getCompletedAt() != null ? Timestamp.valueOf(r.getCompletedAt()) : null,
                r.getResolutionNotes()
            });
        }
        jdbcTemplate.batchUpdate(sql, batch);
    }

    @Transactional
    public void batchInsertGroundTruth(List<GroundTruthAnomaly> list) {
        String sql = "INSERT INTO ground_truth_anomalies (id, entity_type, entity_id, anomaly_type, injected_at, " +
                     "description, expected_action, detected, detection_latency_minutes) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        List<Object[]> batch = new ArrayList<>();
        for (GroundTruthAnomaly g : list) {
            batch.add(new Object[]{
                g.getId(), g.getEntityType(), g.getEntityId(), g.getAnomalyType().name(),
                Timestamp.valueOf(g.getInjectedAt()), g.getDescription(), g.getExpectedAction(),
                g.getDetected() ? 1 : 0, g.getDetectionLatencyMinutes()
            });
        }
        jdbcTemplate.batchUpdate(sql, batch);
    }
}
