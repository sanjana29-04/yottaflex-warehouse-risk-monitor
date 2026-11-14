-- ========================================================================
-- OMNIWAREHOUSE SENTINEL — PRODUCTION MYSQL 8.0 DATABASE SCHEMA
-- ========================================================================

DROP DATABASE IF EXISTS warehouse_db;
CREATE DATABASE warehouse_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE warehouse_db;

SET FOREIGN_KEY_CHECKS = 0;

-- 1. Storage Requirements Master Table
DROP TABLE IF EXISTS storage_requirements;
CREATE TABLE storage_requirements (
    id VARCHAR(32) NOT NULL,
    name VARCHAR(128) NOT NULL,
    category VARCHAR(64) NOT NULL,
    temp_target DOUBLE NOT NULL,
    temp_min DOUBLE NOT NULL,
    temp_max DOUBLE NOT NULL,
    humidity_target DOUBLE NOT NULL,
    humidity_min DOUBLE NOT NULL,
    humidity_max DOUBLE NOT NULL,
    max_door_open_seconds INT NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_storage_cat (category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. Warehouses Master Table
DROP TABLE IF EXISTS warehouses;
CREATE TABLE warehouses (
    id VARCHAR(32) NOT NULL,
    name VARCHAR(128) NOT NULL,
    location VARCHAR(256) NOT NULL,
    capacity_sqm INT NOT NULL,
    total_zones INT NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_wh_status (status),
    INDEX idx_wh_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. Zones Table
DROP TABLE IF EXISTS zones;
CREATE TABLE zones (
    id VARCHAR(48) NOT NULL,
    warehouse_id VARCHAR(32) NOT NULL,
    name VARCHAR(128) NOT NULL,
    zone_type VARCHAR(32) NOT NULL,
    capacity_lots INT NOT NULL,
    current_temperature DOUBLE,
    current_humidity DOUBLE,
    current_occupancy INT NOT NULL DEFAULT 0,
    max_occupancy INT NOT NULL DEFAULT 10,
    temperature_min DOUBLE NOT NULL,
    temperature_max DOUBLE NOT NULL,
    humidity_min DOUBLE NOT NULL,
    humidity_max DOUBLE NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'NORMAL',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_zone_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouses (id) ON DELETE CASCADE,
    INDEX idx_zone_wh (warehouse_id),
    INDEX idx_zone_type (zone_type),
    INDEX idx_zone_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. Sensors Table
DROP TABLE IF EXISTS sensors;
CREATE TABLE sensors (
    id VARCHAR(64) NOT NULL,
    zone_id VARCHAR(48) NOT NULL,
    sensor_type VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    model_number VARCHAR(64),
    installation_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_calibration_date TIMESTAMP NULL,
    calibration_offset DOUBLE NOT NULL DEFAULT 0.0,
    latest_reading_value DOUBLE NULL,
    latest_reading_time TIMESTAMP NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_sensor_zone FOREIGN KEY (zone_id) REFERENCES zones (id) ON DELETE CASCADE,
    INDEX idx_sensor_zone (zone_id),
    INDEX idx_sensor_type (sensor_type),
    INDEX idx_sensor_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. Sensor Readings High-Frequency Telemetry Table
DROP TABLE IF EXISTS sensor_readings;
CREATE TABLE sensor_readings (
    id VARCHAR(80) NOT NULL,
    sensor_id VARCHAR(64) NOT NULL,
    zone_id VARCHAR(48) NOT NULL,
    reading_type VARCHAR(32) NOT NULL,
    reading_value DOUBLE NOT NULL,
    unit VARCHAR(16) NOT NULL,
    timestamp TIMESTAMP NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'NORMAL',
    is_anomaly TINYINT(1) NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT fk_reading_sensor FOREIGN KEY (sensor_id) REFERENCES sensors (id) ON DELETE CASCADE,
    CONSTRAINT fk_reading_zone FOREIGN KEY (zone_id) REFERENCES zones (id) ON DELETE CASCADE,
    INDEX idx_reading_sensor_time (sensor_id, timestamp),
    INDEX idx_reading_zone_time (zone_id, timestamp),
    INDEX idx_reading_type (reading_type),
    INDEX idx_reading_status (status),
    INDEX idx_reading_time (timestamp)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 6. Door Events Table
DROP TABLE IF EXISTS door_events;
CREATE TABLE door_events (
    id VARCHAR(64) NOT NULL,
    zone_id VARCHAR(48) NOT NULL,
    door_name VARCHAR(64) NOT NULL,
    opened_at TIMESTAMP NOT NULL,
    closed_at TIMESTAMP NULL,
    duration_seconds INT NULL,
    max_permitted_seconds INT NOT NULL DEFAULT 300,
    status VARCHAR(32) NOT NULL DEFAULT 'NORMAL',
    alert_triggered TINYINT(1) NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT fk_door_zone FOREIGN KEY (zone_id) REFERENCES zones (id) ON DELETE CASCADE,
    INDEX idx_door_zone (zone_id),
    INDEX idx_door_opened_at (opened_at),
    INDEX idx_door_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 7. Inventory Lots Table
DROP TABLE IF EXISTS inventory_lots;
CREATE TABLE inventory_lots (
    id VARCHAR(32) NOT NULL,
    product_name VARCHAR(128) NOT NULL,
    sku VARCHAR(64) NOT NULL,
    category VARCHAR(64) NOT NULL,
    quantity DOUBLE NOT NULL,
    unit VARCHAR(32) NOT NULL DEFAULT 'PALLETS',
    storage_requirement_id VARCHAR(32) NOT NULL,
    current_zone_id VARCHAR(48) NOT NULL,
    ingress_timestamp TIMESTAMP NOT NULL,
    expiry_date TIMESTAMP NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'STORED',
    exposure_duration_minutes DOUBLE NOT NULL DEFAULT 0.0,
    thermal_stress_score DOUBLE NOT NULL DEFAULT 0.0,
    PRIMARY KEY (id),
    CONSTRAINT fk_lot_requirement FOREIGN KEY (storage_requirement_id) REFERENCES storage_requirements (id),
    CONSTRAINT fk_lot_zone FOREIGN KEY (current_zone_id) REFERENCES zones (id),
    INDEX idx_lot_zone (current_zone_id),
    INDEX idx_lot_req (storage_requirement_id),
    INDEX idx_lot_status (status),
    INDEX idx_lot_sku (sku)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 8. Lot Zone Assignment History (Dwell Times Bridge Table)
DROP TABLE IF EXISTS lot_zone_assignments;
CREATE TABLE lot_zone_assignments (
    id VARCHAR(64) NOT NULL,
    lot_id VARCHAR(32) NOT NULL,
    zone_id VARCHAR(48) NOT NULL,
    ingress_timestamp TIMESTAMP NOT NULL,
    egress_timestamp TIMESTAMP NULL,
    dwell_minutes BIGINT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_lza_lot FOREIGN KEY (lot_id) REFERENCES inventory_lots (id) ON DELETE CASCADE,
    CONSTRAINT fk_lza_zone FOREIGN KEY (zone_id) REFERENCES zones (id) ON DELETE CASCADE,
    INDEX idx_lza_lot (lot_id),
    INDEX idx_lza_zone (zone_id),
    INDEX idx_lza_ingress (ingress_timestamp)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 9. Alerts Table
DROP TABLE IF EXISTS alerts;
CREATE TABLE alerts (
    id VARCHAR(64) NOT NULL,
    zone_id VARCHAR(48) NOT NULL,
    sensor_id VARCHAR(64) NULL,
    alert_type VARCHAR(32) NOT NULL,
    risk_level VARCHAR(16) NOT NULL DEFAULT 'MEDIUM',
    status VARCHAR(32) NOT NULL DEFAULT 'OPEN',
    triggered_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMP NULL,
    trigger_value DOUBLE NULL,
    threshold_value DOUBLE NULL,
    message VARCHAR(512) NOT NULL,
    acknowledged_by VARCHAR(64) NULL,
    resolved_by VARCHAR(64) NULL,
    notes VARCHAR(1024) NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_alert_zone FOREIGN KEY (zone_id) REFERENCES zones (id) ON DELETE CASCADE,
    CONSTRAINT fk_alert_sensor FOREIGN KEY (sensor_id) REFERENCES sensors (id) ON DELETE SET NULL,
    INDEX idx_alert_zone (zone_id),
    INDEX idx_alert_status (status),
    INDEX idx_alert_risk (risk_level),
    INDEX idx_alert_time (triggered_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 10. Alarm Lot Impact (Spatiotemporal Intersection Bridge Table)
DROP TABLE IF EXISTS alarm_lot_impacts;
CREATE TABLE alarm_lot_impacts (
    id VARCHAR(64) NOT NULL,
    alert_id VARCHAR(64) NOT NULL,
    lot_id VARCHAR(32) NOT NULL,
    exposure_minutes DOUBLE NOT NULL DEFAULT 0.0,
    thermal_stress_score DOUBLE NOT NULL DEFAULT 0.0,
    action_recommended VARCHAR(128) NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_ali_alert FOREIGN KEY (alert_id) REFERENCES alerts (id) ON DELETE CASCADE,
    CONSTRAINT fk_ali_lot FOREIGN KEY (lot_id) REFERENCES inventory_lots (id) ON DELETE CASCADE,
    INDEX idx_ali_alert (alert_id),
    INDEX idx_ali_lot (lot_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 11. Inspections Table
DROP TABLE IF EXISTS inspections;
CREATE TABLE inspections (
    id VARCHAR(32) NOT NULL,
    zone_id VARCHAR(48) NOT NULL,
    alert_id VARCHAR(64) NULL,
    inspector_name VARCHAR(64) NOT NULL,
    inspection_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    observations VARCHAR(1024) NULL,
    findings_summary VARCHAR(512) NULL,
    action_required TINYINT(1) NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT fk_inspection_zone FOREIGN KEY (zone_id) REFERENCES zones (id) ON DELETE CASCADE,
    CONSTRAINT fk_inspection_alert FOREIGN KEY (alert_id) REFERENCES alerts (id) ON DELETE SET NULL,
    INDEX idx_insp_zone (zone_id),
    INDEX idx_insp_status (status),
    INDEX idx_insp_date (inspection_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 12. Remediations Table
DROP TABLE IF EXISTS remediations;
CREATE TABLE remediations (
    id VARCHAR(32) NOT NULL,
    alert_id VARCHAR(64) NOT NULL,
    inspection_id VARCHAR(32) NULL,
    lot_id VARCHAR(32) NULL,
    action_title VARCHAR(128) NOT NULL,
    action_type VARCHAR(32) NOT NULL,
    assigned_to VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'SCHEDULED',
    cost_impact DOUBLE NOT NULL DEFAULT 0.0,
    scheduled_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP NULL,
    resolution_notes VARCHAR(1024) NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_remediation_alert FOREIGN KEY (alert_id) REFERENCES alerts (id) ON DELETE CASCADE,
    CONSTRAINT fk_remediation_inspection FOREIGN KEY (inspection_id) REFERENCES inspections (id) ON DELETE SET NULL,
    CONSTRAINT fk_remediation_lot FOREIGN KEY (lot_id) REFERENCES inventory_lots (id) ON DELETE SET NULL,
    INDEX idx_rem_alert (alert_id),
    INDEX idx_rem_status (status),
    INDEX idx_rem_type (action_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 13. Ground Truth Anomalies Table (Step 7 Evaluation Isolation)
DROP TABLE IF EXISTS ground_truth_anomalies;
CREATE TABLE ground_truth_anomalies (
    id VARCHAR(64) NOT NULL,
    entity_type VARCHAR(32) NOT NULL,
    entity_id VARCHAR(64) NOT NULL,
    anomaly_type VARCHAR(48) NOT NULL,
    injected_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    description VARCHAR(512) NOT NULL,
    expected_action VARCHAR(256) NOT NULL,
    detected TINYINT(1) NOT NULL DEFAULT 0,
    detection_latency_minutes DOUBLE NULL,
    PRIMARY KEY (id),
    INDEX idx_gta_type (anomaly_type),
    INDEX idx_gta_entity (entity_type, entity_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET FOREIGN_KEY_CHECKS = 1;
