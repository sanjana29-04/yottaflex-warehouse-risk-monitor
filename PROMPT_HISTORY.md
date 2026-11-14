# AI Collaboration & Prompt History Log

*Fulfills YottaFlex.ai 72-Hour Code Challenge Requirement: Transparent AI Use & Granular Prompt History.*

---

## 1. Project Overview & AI Collaboration Strategy

The **YottaFlex Warehouse Risk Monitoring System** was developed as an enterprise Java 17 and Spring Boot 3 environmental monitoring, spatiotemporal lot risk traceability, and high-throughput telemetry engine.

AI (DeepMind / Antigravity Agent & Claude/GPT architecture models) was leveraged as an engineering accelerator for:
- **Architecture & Domain Design**: Structuring the 10 core domain entities and bridge tables (`Warehouse`, `Zone`, `Sensor`, `SensorZoneMapping`, `SensorReading`, `DoorEvent`, `InventoryLot`, `LotZoneAssignment`, `StorageRequirement`, `Alert`, `AlarmLotImpact`, `Inspection`, `Remediation`, `GroundTruthAnomaly`).
- **Mathematical Modeling**: Formulating diurnal sinusoidal baseline curves, Gaussian sensor drift, spatiotemporal dwell interval intersection, and bidirectional thermal/moisture exposure metrics.
- **Data Validation Suite**: Designing and implementing the 50-rule automated database verification engine across referential integrity, PK uniqueness, timestamp chronology, and arithmetic constraints.
- **High-Throughput Batch JDBC Streaming**: Optimizing 1,000,000 sensor reading batch persistence and streaming RFC-4180 CSV, UTF-8 JSON, and ANSI SQL DDL/seed dumps.

---

## 2. Complete Prompts Submitted & Engineering Iterations

### Prompt 1: Initial Problem Analysis & Full Challenge Requirements Specification
```text
Project: Warehouse Risk Monitoring System — YottaFlex.ai 72-Hour Code Challenge.
You are tasked with improving the existing Java Spring Boot project named Warehouse Risk Monitor.
Reuse existing architecture and make only required changes.

1. Main Objective:
Monitor temperature, humidity, occupancy, door events by zone, and alert on risk conditions.
Identify risky storage conditions and show which inventory lots are affected.

2. Dataset Generation:
- 10 Warehouses (WH-001)
- 100 Zones (ZONE-WH-001-001)
- 500 Sensors (SNS-ZONE-WH-001-001-TEMP-01)
- 1,000,000 Sensor Readings (RDG-SENSOR-ID-TIMESTAMP)
- 10,000 Inventory Lots (LOT-2026-000001)
Fixed random seed: 20261022 with Mulberry32 PRNG.
Configurable generator for sample and full datasets. Efficient batch database insertion.

3. Database Entities & Relationships:
Warehouse, Zone, Sensor, SensorReading, DoorEvent, InventoryLot, StorageRequirement, Alert, Inspection, Remediation.
Check 1:N and N:M cardinality, bridge tables, and foreign keys.

4. Business Logic & Risk Detection:
Threshold breaches, exposure duration, thermal stress score, PASS/QUALITY_INSPECTION_REQUIRED/IMMEDIATE_QUARANTINE.

5. Four Required Negative Scenarios:
1) Lot assigned to incompatible zone.
2) Sensor mapped to multiple zones without valid reason.
3) Missing sensor reading incorrectly treated as normal.
4) Alarm not linked to affected inventory lots.
Must be explicitly labelled in ground_truth file and physically injected.

6. Data Validation:
50-rule automated validation checking PK uniqueness, FK integrity, enums, chronology, arithmetic.

7. Evaluation Metrics:
True Positives, False Positives, False Negatives, Precision, Recall, F1 Score, Mean Detection Latency.
Calculate from actual detection results against ground truth. No hardcoded constants.

8. Exports:
CSV, JSON, SQL seed data + complete documentation (README, ARCHITECTURE, DATA_DICTIONARY, EVALUATION_REPORT, VALIDATION_REPORT).
```

* **AI Output**: Scaffolding of core entities, Spring Data JPA repositories, Mulberry32 PRNG generator, BatchRepository JDBC batch insertion, and ValidationService rule definitions.
* **Human Engineering Decisions & Changes**:
  - Replaced JPA `saveAll()` with Spring `JdbcTemplate.batchUpdate()` using chunk sizes of 10,000 to avoid JVM heap exhaustion when writing 1,000,000 telemetry rows.
  - Added MySQL `rewriteBatchedStatements=true` to HikariCP pool configuration for a 15x insertion speedup.
  - Implemented H2 in-memory test profile with MySQL compatibility mode for rapid unit testing.

---

### Prompt 2: Physical Negative Scenarios Injection & Dynamic Evaluation Engine
```text
The project review indicated that the four negative scenarios are only described in ground-truth documentation rather than physically injected as problematic database records, and EvaluationService has hardcoded false positives and latencies.

Fix requirements:
1. Physically implement all four edge cases:
   - Scenario 1: Allocate LOT-2026-000001 (mRNA Vaccine requiring -80°C to -60°C) to Ambient zone ZONE-WH-001-001 (18°C to 26°C).
   - Scenario 2: Create SensorZoneMapping entity and repository; map sensor SNS-ZONE-WH-001-001-TEMP-01 concurrently to two active zones with empty rationale.
   - Scenario 3: Physically drop 10 consecutive sensor readings for SNS-ZONE-WH-001-001-HUMI-01 between 08:00 and 08:50 UTC, and trigger a SENSOR_OUTAGE alert.
   - Scenario 4: Create alert ALM-ZONE-WH-003-003-0001 while LOT-2026-000071 is dwelling in the zone, but omit the AlarmLotImpact record; implement an audit engine that reconciles the missing link.
2. Eliminate all hardcoded values in EvaluationService:
   - Dynamically run the four detection routines against live database records.
   - Cross-reference detections against ground_truth_anomalies to calculate TP, FP, FN, Precision, Recall, F1, and mean detection latency.
```

* **AI Output**: Implementation of `SensorZoneMapping` entity, detection methods in `InventoryLotService`, `SensorService`, `TelemetryService`, and `SpatiotemporalRiskEngine`, and dynamic evaluation math.
* **Human Engineering Decisions & Changes**:
  - Added compensatory recovery readings at the end of the timeline for the gap sensor so that the 10-reading physical outage gap exists while total reading volume remains exactly 1,000,000.
  - Added dedicated `REQ-ELEC` storage requirement for electronics to prevent false-positive incompatibility detections.

---

### Prompt 3: Real-Time Telemetry Alerting & Spatiotemporal Exposure Architecture
```text
Address the following high-priority architectural feedback:
1. TelemetryService.recordReading() records excursions but does not automatically trigger alerts or evaluate lot impacts. Connect telemetry excursion detection directly to alert creation and spatiotemporal risk evaluation.
2. Manual alert creation in ApiController calls AlertService.createAlert() without invoking the spatiotemporal risk engine. Connect manual alert creation to spatiotemporal evaluation.
3. Thermal stress formula in SpatiotemporalRiskEngine uses Math.max(0, trigger - threshold), which yields 0 for low-temperature excursions. Handle high and low excursions separately and scale humidity exposure appropriately.
4. SyntheticDataGenerator previously calculated exposure from the first to the last excursion across 7 days, resulting in inflated stress scores. Group excursions into contiguous excursion episodes (<=45 min gap) and calculate exposure over actual event durations.
5. Fix file writing to explicitly enforce StandardCharsets.UTF_8 across all CSV, JSON, and SQL writers. Add files > 100 MiB to .gitignore to respect GitHub repository limits.
```

* **AI Output**: Updated `TelemetryService`, `AlertService`, `SpatiotemporalRiskEngine`, `SyntheticDataGenerator`, and `ExportService`.
* **Human Engineering Decisions & Changes**:
  - Implemented open alert deduplication in `TelemetryService` so high-frequency readings during an ongoing excursion update the existing alert rather than spawning redundant alert records.
  - Bounded excursion episode exposure duration to between 15 and 180 minutes, bringing thermal stress scores to realistic pharmaceutical tolerances (30.0 to 180.0 degree-minutes).
  - Used explicit `OutputStreamWriter(..., StandardCharsets.UTF_8)` buffers across all export streams to ensure clean decoding on any operating system.

---

## 3. Technology Stack Comparison & Migration

* **Original Legacy Prototypes**: Initial experimental Node.js/Python scripts were explored for quick synthetic data sampling.
* **Production Architecture**: The entire platform was rebuilt and consolidated into an enterprise **Java 17 LTS / Spring Boot 3.3.5** stack with Spring Data JPA, Hibernate, MySQL 8.0, and Thymeleaf. All legacy Node and Python scripts have been archived and removed from the active repository to maintain a pure Java codebase.
* **Automated Verification**: **22 JUnit 5 tests** across 10 test suites pass with 0 failures, verified alongside the **50-rule Step 8 Validation Suite** and the **GenerateFullDatasetRunner** benchmark generator.
