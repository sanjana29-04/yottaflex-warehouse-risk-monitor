# YottaFlex Warehouse Monitoring System — Data Dictionary

This document details the database schema, entity attributes, types, constraints, and relationships for the **YottaFlex Warehouse Monitoring System**.

---

## 1. Core Entities Summary

| Entity | Table Name | Primary Key | Description | Relationships |
|:---|:---|:---|:---|:---|
| **Warehouse** | `warehouses` | `id` | Physical logistics distribution facility | $1 : N$ with `zones` |
| **Zone** | `zones` | `id` | Atmospheric storage zone | $N : 1$ with `warehouses`, $1 : N$ with `sensors`, `inventory_lots` |
| **Sensor** | `sensors` | `id` | IoT environmental telemetry hardware | $N : 1$ with `zones`, $1 : N$ with `sensor_readings` |
| **SensorReading**| `sensor_readings` | `id` | High-frequency telemetry stream sample | $N : 1$ with `sensors`, $N : 1$ with `zones` |
| **DoorEvent** | `door_events` | `id` | Loading dock door open/close tracking | $N : 1$ with `zones` |
| **StorageRequirement** | `storage_requirements` | `id` | Product category thermal/humidity specifications | $1 : N$ with `inventory_lots` |
| **InventoryLot** | `inventory_lots` | `id` | Manufactured batch inventory in storage | $N : 1$ with `storage_requirements`, $N : 1$ with `zones` |
| **LotZoneAssignment** | `lot_zone_assignments` | `id` | Historical lot dwell in zones | $N : 1$ with `inventory_lots`, $N : 1$ with `zones` |
| **Alert** | `alerts` | `id` | Environmental excursion alarm | $N : 1$ with `zones`, $N : 1$ with `sensors`, $1 : N$ with `inspections`, `remediations` |
| **AlarmLotImpact** | `alarm_lot_impacts` | `id` | Spatiotemporal exposure bridge | $N : 1$ with `alerts`, $N : 1$ with `inventory_lots` |
| **Inspection** | `inspections` | `id` | Physical quality assurance site walkthrough | $N : 1$ with `zones`, $N : 1$ with `alerts` |
| **Remediation** | `remediations` | `id` | Corrective maintenance action order | $N : 1$ with `alerts`, $N : 1$ with `inspections` |
| **SensorZoneMapping** | `sensor_zone_mappings` | `id` | Topology mapping of sensors to active warehouse zones | $N : 1$ with `sensors`, $N : 1$ with `zones` |
| **GroundTruthAnomaly** | `ground_truth_anomalies` | `id` | Isolated ground-truth anomaly benchmark | Independent validation table |

---

## 2. Detailed Field Specifications

### 2.1 Warehouse (`warehouses`)
*Nomenclature*: `WH-001` through `WH-010`

| Column | Type | Nullable | Description / Allowed Values |
|:---|:---|:---:|:---|
| `id` | `VARCHAR(32)` | No | Primary Key (`WH-001`) |
| `name` | `VARCHAR(128)` | No | Facility name (e.g. `YottaFlex WH-001`) |
| `location` | `VARCHAR(128)` | No | City and jurisdiction (e.g. `Chicago, IL`) |
| `capacity_sqm` | `INT` | No | Total facility footprint ($15,000$ to $60,000\,\text{m}^2$) |
| `total_zones` | `INT` | No | Configured zone count (10 in full scale) |
| `status` | `VARCHAR(32)` | No | `ACTIVE`, `INACTIVE`, `MAINTENANCE` |

---

### 2.2 Zone (`zones`)
*Nomenclature*: `ZONE-WH-001-001`

| Column | Type | Nullable | Description / Allowed Values |
|:---|:---|:---:|:---|
| `id` | `VARCHAR(64)` | No | Primary Key |
| `warehouse_id` | `VARCHAR(32)` | No | Foreign Key $\rightarrow$ `warehouses(id)` |
| `name` | `VARCHAR(128)` | No | Zone label |
| `zone_type` | `VARCHAR(32)` | No | `COLD_STORAGE`, `FROZEN`, `AMBIENT`, `HAZARDOUS`, `BUFFER` |
| `capacity_lots` | `INT` | No | Maximum lot capacity ($100$ to $800$ lots) |
| `temperature_min` | `DOUBLE` | No | Minimum temperature threshold ($^\circ\text{C}$) |
| `temperature_max` | `DOUBLE` | No | Maximum temperature threshold ($^\circ\text{C}$) |
| `humidity_min` | `DOUBLE` | No | Minimum relative humidity threshold ($\%$) |
| `humidity_max` | `DOUBLE` | No | Maximum relative humidity threshold ($\%$) |
| `max_occupancy` | `INT` | No | Maximum personnel allowed |
| `current_temperature` | `DOUBLE` | Yes | Real-time live temperature reading |
| `current_humidity` | `DOUBLE` | Yes | Real-time live humidity reading |
| `current_occupancy` | `INT` | Yes | Real-time zone personnel count |
| `status` | `VARCHAR(32)` | No | `NORMAL`, `WARNING`, `CRITICAL` |

---

### 2.3 Sensor (`sensors`)
*Nomenclature*: `SNS-ZONE-WH-001-001-TEMP-01`

| Column | Type | Nullable | Description / Allowed Values |
|:---|:---|:---:|:---|
| `id` | `VARCHAR(80)` | No | Primary Key |
| `zone_id` | `VARCHAR(64)` | No | Foreign Key $\rightarrow$ `zones(id)` |
| `sensor_type` | `VARCHAR(32)` | No | `TEMPERATURE`, `HUMIDITY`, `OCCUPANCY`, `DOOR` |
| `status` | `VARCHAR(32)` | No | `ACTIVE`, `INACTIVE`, `DRIFTING`, `CALIBRATING` |
| `model_number` | `VARCHAR(64)` | No | Hardware model number |
| `installation_date` | `DATETIME` | Yes | Sensor commissioning timestamp |
| `last_calibration_date`| `DATETIME` | Yes | Last maintenance calibration date |
| `calibration_offset` | `DOUBLE` | No | Calibration delta adjustment |
| `latest_reading_value` | `DOUBLE` | Yes | Most recent measured reading |
| `latest_reading_time` | `DATETIME` | Yes | Timestamp of most recent reading |

---

### 2.4 SensorReading (`sensor_readings`)
*Nomenclature*: `RDG-SENSOR-ID-TIMESTAMP`

| Column | Type | Nullable | Description / Allowed Values |
|:---|:---|:---:|:---|
| `id` | `VARCHAR(80)` | No | Primary Key |
| `sensor_id` | `VARCHAR(80)` | No | Foreign Key $\rightarrow$ `sensors(id)` |
| `zone_id` | `VARCHAR(64)` | No | Foreign Key $\rightarrow$ `zones(id)` |
| `reading_type` | `VARCHAR(32)` | No | `TEMPERATURE`, `HUMIDITY`, `OCCUPANCY`, `DOOR` |
| `reading_value` | `DOUBLE` | No | Measured numeric value |
| `unit` | `VARCHAR(16)` | No | Metric unit: `°C`, `%`, `COUNT`, `STATE` |
| `timestamp` | `DATETIME` | No | Timestamp of reading (indexed) |
| `status` | `VARCHAR(32)` | No | `NORMAL`, `EXCURSION`, `WARNING`, `OUTLIER` |
| `is_anomaly` | `BOOLEAN` | No | `true` if excursion / threshold breach |

---

### 2.5 DoorEvent (`door_events`)
*Nomenclature*: `DEV-ZONE-ID-SEQUENCE`

| Column | Type | Nullable | Description / Allowed Values |
|:---|:---|:---:|:---|
| `id` | `VARCHAR(64)` | No | Primary Key |
| `zone_id` | `VARCHAR(64)` | No | Foreign Key $\rightarrow$ `zones(id)` |
| `door_name` | `VARCHAR(64)` | No | Door bay identifier |
| `opened_at` | `DATETIME` | No | Ingress open timestamp |
| `closed_at` | `DATETIME` | Yes | Latch close timestamp |
| `duration_seconds` | `INT` | Yes | Computed open duration in seconds |
| `max_permitted_seconds`| `INT` | No | Permitted SLA duration (e.g. 180s) |
| `status` | `VARCHAR(32)` | No | `NORMAL`, `EXCEEDED_DURATION` |
| `alert_triggered` | `BOOLEAN` | No | `true` if duration exceeded SLA |

---

### 2.6 StorageRequirement (`storage_requirements`)
*Nomenclature*: `REQ-CATEGORY`

| Column | Type | Nullable | Description / Allowed Values |
|:---|:---|:---:|:---|
| `id` | `VARCHAR(32)` | No | Primary Key |
| `name` | `VARCHAR(128)` | No | Requirement specification name |
| `category` | `VARCHAR(32)` | No | `PHARMACEUTICAL`, `VACCINES`, `DAIRY`, `FROZEN_FOODS`, `PRODUCE`, `ELECTRONICS`, `GENERAL_MERCHANDISE` |
| `temp_target` | `DOUBLE` | No | Target optimal storage temperature ($^\circ\text{C}$) |
| `temp_min` | `DOUBLE` | No | Minimum allowable temperature ($^\circ\text{C}$) |
| `temp_max` | `DOUBLE` | No | Maximum allowable temperature ($^\circ\text{C}$) |
| `humidity_target` | `DOUBLE` | No | Target optimal relative humidity ($\%$) |
| `humidity_min` | `DOUBLE` | No | Minimum allowable relative humidity ($\%$) |
| `humidity_max` | `DOUBLE` | No | Maximum allowable relative humidity ($\%$) |
| `max_door_open_seconds`| `INT` | No | Maximum permitted door opening duration (seconds) |

---

### 2.7 InventoryLot (`inventory_lots`)
*Nomenclature*: `LOT-2026-000001` through `LOT-2026-010000`

| Column | Type | Nullable | Description / Allowed Values |
|:---|:---|:---:|:---|
| `id` | `VARCHAR(64)` | No | Primary Key |
| `product_name` | `VARCHAR(128)` | No | Product commercial name |
| `sku` | `VARCHAR(64)` | No | Stock keeping unit |
| `category` | `VARCHAR(32)` | No | ProductCategory enum |
| `quantity` | `DOUBLE` | No | Quantity in storage |
| `unit` | `VARCHAR(32)` | No | `PALLETS`, `BOXES`, `KG`, `VIALS`, `DRUMS` |
| `storage_requirement_id`| `VARCHAR(32)`| No | Foreign Key $\rightarrow$ `storage_requirements(id)` |
| `current_zone_id` | `VARCHAR(64)` | No | Foreign Key $\rightarrow$ `zones(id)` |
| `ingress_timestamp` | `DATETIME` | No | Receipt into facility timestamp |
| `expiry_date` | `DATETIME` | No | Product expiration date |
| `exposure_duration_minutes`| `DOUBLE` | No | Cumulative exposure to environmental excursions |
| `thermal_stress_score` | `DOUBLE` | No | Cumulative degree-minute thermal dose |
| `status` | `VARCHAR(32)` | No | `STORED`, `QUARANTINED`, `RELEASED`, `DISPOSED` |

---

### 2.8 Alert (`alerts`)
*Nomenclature*: `ALM-ZONE-ID-SEQUENCE`

| Column | Type | Nullable | Description / Allowed Values |
|:---|:---|:---:|:---|
| `id` | `VARCHAR(64)` | No | Primary Key |
| `zone_id` | `VARCHAR(64)` | No | Foreign Key $\rightarrow$ `zones(id)` |
| `sensor_id` | `VARCHAR(80)` | Yes | Foreign Key $\rightarrow$ `sensors(id)` |
| `alert_type` | `VARCHAR(32)` | No | `HIGH_TEMPERATURE`, `LOW_TEMPERATURE`, `HIGH_HUMIDITY`, `LOW_HUMIDITY`, `OCCUPANCY_EXCEEDED`, `DOOR_OPEN_PROLONGED`, `STORAGE_INCOMPATIBILITY` |
| `risk_level` | `VARCHAR(32)` | No | `LOW`, `MEDIUM`, `HIGH`, `CRITICAL` |
| `status` | `VARCHAR(32)` | No | `OPEN`, `ACKNOWLEDGED`, `RESOLVED` |
| `triggered_at` | `DATETIME` | No | Alarm trigger timestamp |
| `acknowledged_at` | `DATETIME` | Yes | Supervisor acknowledgment timestamp |
| `resolved_at` | `DATETIME` | Yes | Resolution completion timestamp |
| `trigger_value` | `DOUBLE` | Yes | Measured excursion reading |
| `threshold_value` | `DOUBLE` | Yes | Regulatory limit breached |
| `acknowledged_by` | `VARCHAR(64)` | Yes | Operator username |
| `resolved_by` | `VARCHAR(64)` | Yes | Quality engineer username |
| `message` | `VARCHAR(255)` | Yes | Excursion summary message |
| `notes` | `TEXT` | Yes | Corrective notes |

---

### 2.9 Inspection (`inspections`)
*Nomenclature*: `INSP-2026-000001`

| Column | Type | Nullable | Description / Allowed Values |
|:---|:---|:---:|:---|
| `id` | `VARCHAR(64)` | No | Primary Key |
| `zone_id` | `VARCHAR(64)` | No | Foreign Key $\rightarrow$ `zones(id)` |
| `alert_id` | `VARCHAR(64)` | Yes | Foreign Key $\rightarrow$ `alerts(id)` |
| `inspector_name` | `VARCHAR(64)` | No | Quality auditor name |
| `scheduled_at` | `DATETIME` | No | Scheduled walkthrough timestamp |
| `completed_at` | `DATETIME` | Yes | Completion timestamp |
| `status` | `VARCHAR(32)` | No | `SCHEDULED`, `IN_PROGRESS`, `COMPLETED` |
| `findings` | `TEXT` | Yes | Physical audit findings |
| `corrective_actions` | `TEXT` | Yes | Prescribed remediation |
| `passed` | `BOOLEAN` | No | `true` if inspection passed |

---

### 2.10 Remediation (`remediations`)
*Nomenclature*: `REM-2026-000001`

| Column | Type | Nullable | Description / Allowed Values |
|:---|:---|:---:|:---|
| `id` | `VARCHAR(64)` | No | Primary Key |
| `alert_id` | `VARCHAR(64)` | Yes | Foreign Key $\rightarrow$ `alerts(id)` |
| `inspection_id` | `VARCHAR(64)` | Yes | Foreign Key $\rightarrow$ `inspections(id)` |
| `affected_lot_id` | `VARCHAR(64)` | Yes | Foreign Key $\rightarrow$ `inventory_lots(id)` |
| `action_title` | `VARCHAR(128)` | No | Remediation title |
| `action_type` | `VARCHAR(32)` | No | `HVAC_RESET`, `COMPRESSOR_REPAIR`, `DOOR_SEAL_REPLACEMENT`, `INVENTORY_RELOCATION`, `SENSOR_CALIBRATION`, `EQUIPMENT_REPAIR` |
| `assigned_to` | `VARCHAR(64)` | No | Maintenance technician |
| `status` | `VARCHAR(32)` | No | `PENDING`, `IN_PROGRESS`, `COMPLETED` |
| `cost_impact` | `DOUBLE` | No | Repair expenditure in USD |
| `scheduled_at` | `DATETIME` | No | Scheduled repair timestamp |
| `completed_at` | `DATETIME` | Yes | Repair sign-off timestamp |
| `resolution_notes` | `TEXT` | Yes | Repair summary notes |

---

### 2.11 GroundTruthAnomaly (`ground_truth_anomalies`)
*Nomenclature*: `GTA-001` through `GTA-004`

| Column | Type | Nullable | Description / Allowed Values |
|:---|:---|:---:|:---|
| `id` | `VARCHAR(32)` | No | Primary Key |
| `entity_type` | `VARCHAR(64)` | No | `InventoryLot`, `Sensor`, `SensorReading`, `Alarm` |
| `entity_id` | `VARCHAR(80)` | No | Target entity ID |
| `anomaly_type` | `VARCHAR(64)` | No | `LOT_ASSIGNED_INCOMPATIBLE_ZONE`, `SENSOR_MAPPED_MULTIPLE_ZONES`, `UNREPORTED_SENSOR_READING_GAP`, `ALARM_NOT_LINKED_TO_AFFECTED_LOT` |
| `timestamp` | `DATETIME` | No | Anomaly injection timestamp |
| `description` | `TEXT` | Yes | Anomaly diagnostic description |
| `expected_action` | `VARCHAR(128)` | Yes | Prescribed corrective outcome |
| `detected` | `BOOLEAN` | No | `true` if detected by system engine |
| `detection_latency_minutes`| `DOUBLE` | Yes | Detection latency in minutes |

---

### 2.12 SensorZoneMapping (`sensor_zone_mappings`)
*Nomenclature*: `SZM-ZONE-ID-SNS-ID-SEQ`

| Column | Type | Nullable | Description / Allowed Values |
|:---|:---|:---:|:---|
| `id` | `VARCHAR(80)` | No | Primary Key |
| `sensor_id` | `VARCHAR(80)` | No | Foreign Key $\rightarrow$ `sensors(id)` |
| `zone_id` | `VARCHAR(64)` | No | Foreign Key $\rightarrow$ `zones(id)` |
| `is_active` | `BOOLEAN` | No | Whether the mapping is active |
| `mapped_at` | `DATETIME` | No | Commissioning timestamp of mapping |
| `rationale` | `VARCHAR(255)` | Yes | Engineering rationale for mapping |

---

### 2.13 LotZoneAssignment (`lot_zone_assignments`)
*Nomenclature*: `LZA-LOT-ID-ZONE-ID-SEQ`

| Column | Type | Nullable | Description / Allowed Values |
|:---|:---|:---:|:---|
| `id` | `VARCHAR(80)` | No | Primary Key |
| `lot_id` | `VARCHAR(64)` | No | Foreign Key $\rightarrow$ `inventory_lots(id)` |
| `zone_id` | `VARCHAR(64)` | No | Foreign Key $\rightarrow$ `zones(id)` |
| `assigned_at` | `DATETIME` | No | Dwell ingress timestamp |
| `unassigned_at` | `DATETIME` | Yes | Dwell egress timestamp |
| `is_current` | `BOOLEAN` | No | Whether lot is currently dwelling in this zone |

---

### 2.14 AlarmLotImpact (`alarm_lot_impacts`)
*Nomenclature*: `ALI-ALM-ID-LOT-ID-SEQ`

| Column | Type | Nullable | Description / Allowed Values |
|:---|:---|:---:|:---|
| `id` | `VARCHAR(80)` | No | Primary Key |
| `alert_id` | `VARCHAR(64)` | No | Foreign Key $\rightarrow$ `alerts(id)` |
| `lot_id` | `VARCHAR(64)` | No | Foreign Key $\rightarrow$ `inventory_lots(id)` |
| `exposure_duration_minutes` | `DOUBLE` | No | Overlap duration during excursion |
| `thermal_stress_score` | `DOUBLE` | No | Degree-minute thermal stress score |
| `recommended_action` | `VARCHAR(64)` | No | `PASS`, `QUALITY_INSPECTION_REQUIRED`, `IMMEDIATE_QUARANTINE` |
