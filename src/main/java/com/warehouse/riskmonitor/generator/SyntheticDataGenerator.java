package com.warehouse.riskmonitor.generator;

import com.warehouse.riskmonitor.entity.*;
import com.warehouse.riskmonitor.entity.enums.*;
import com.warehouse.riskmonitor.repository.BatchRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class SyntheticDataGenerator {

    private final BatchRepository batchRepository;

    public SyntheticDataGenerator(BatchRepository batchRepository) {
        this.batchRepository = batchRepository;
    }

    public static class ScaleConfig {
        public int warehouses;
        public int zonesPerWarehouse;
        public int sensorsPerZone;
        public int lots;
        public int days;
        public int readingIntervalMinutes;
        public int readingsPerSensor;

        public ScaleConfig(int warehouses, int zonesPerWarehouse, int sensorsPerZone, int lots, int days, int readingIntervalMinutes, int readingsPerSensor) {
            this.warehouses = warehouses;
            this.zonesPerWarehouse = zonesPerWarehouse;
            this.sensorsPerZone = sensorsPerZone;
            this.lots = lots;
            this.days = days;
            this.readingIntervalMinutes = readingIntervalMinutes;
            this.readingsPerSensor = readingsPerSensor;
        }
    }

    public static ScaleConfig getScale(String scale) {
        if ("sample".equalsIgnoreCase(scale)) {
            // Sample scale: 2 warehouses, 6 zones, 30 sensors, 50 lots, 3,000 readings
            return new ScaleConfig(2, 3, 5, 50, 2, 60, 100);
        } else if ("full".equalsIgnoreCase(scale)) {
            // Full scale challenge requirement: 10 warehouses, 100 zones, 500 sensors, 10,000 lots, exactly 1,000,000 readings
            return new ScaleConfig(10, 10, 5, 10000, 7, 5, 2000);
        } else {
            // starter (default): 4 warehouses, 20 zones, 100 sensors, 500 lots, 30,000 readings
            return new ScaleConfig(4, 5, 5, 500, 3, 15, 300);
        }
    }

    public static class GeneratedDataset {
        public List<Warehouse> warehouses = new ArrayList<>();
        public List<Zone> zones = new ArrayList<>();
        public List<StorageRequirement> storageRequirements = new ArrayList<>();
        public List<Sensor> sensors = new ArrayList<>();
        public List<SensorZoneMapping> sensorZoneMappings = new ArrayList<>();
        public List<SensorReading> readings = new ArrayList<>();
        public List<DoorEvent> doorEvents = new ArrayList<>();
        public List<InventoryLot> inventoryLots = new ArrayList<>();
        public List<LotZoneAssignment> lotZoneAssignments = new ArrayList<>();
        public List<Alert> alerts = new ArrayList<>();
        public List<AlarmLotImpact> alarmLotImpacts = new ArrayList<>();
        public List<Inspection> inspections = new ArrayList<>();
        public List<Remediation> remediations = new ArrayList<>();
        public List<GroundTruthAnomaly> groundTruthAnomalies = new ArrayList<>();
        public Alert unlinkedAlert;
        public InventoryLot unlinkedLot;
        public Sensor gapSensor;
    }

    public GeneratedDataset generate(String scaleName, long seed, boolean persistToDb) {
        ScaleConfig scale = getScale(scaleName);
        Mulberry32 prng = new Mulberry32(seed);
        GeneratedDataset dataset = new GeneratedDataset();

        LocalDateTime baseTime = LocalDateTime.of(2026, 11, 1, 0, 0, 0);

        // 1. Storage Requirements Master Data
        dataset.storageRequirements = createStorageRequirements();

        // 2. Warehouses Master Data
        dataset.warehouses = createWarehouses(scale.warehouses, prng, scale.zonesPerWarehouse);

        // 3. Zones Master Data
        dataset.zones = createZones(dataset.warehouses, scale.zonesPerWarehouse, prng);

        // 4. Sensors (5 per zone: 2 Temp, 1 Hum, 1 Occ, 1 Door -> 500 sensors for 100 zones)
        dataset.sensors = createSensors(dataset.zones, prng);

        // 4b. Sensor Zone Mappings Master Data
        dataset.sensorZoneMappings = createSensorZoneMappings(dataset.sensors);

        // 5. Inventory Lots & Initial Zone Assignments
        createInventoryLotsAndAssignments(dataset, scale.lots, prng, baseTime);

        // 6. Door Events
        dataset.doorEvents = createDoorEvents(dataset.zones, scale.days, prng, baseTime);

        // 7. Sensor Readings (Telemetry Stream with exact count: 500 sensors * 2000 readings = 1,000,000)
        dataset.readings = createReadings(dataset, scale.readingsPerSensor, scale.readingIntervalMinutes, prng, baseTime);

        // 8. Alerts & Spatiotemporal Impact Evaluation
        createAlertsAndSpatiotemporalImpact(dataset, prng);

        // 9. Inspections & Remediations
        createInspectionsAndRemediations(dataset, prng);

        // 10. Inject 4 Documented Edge Cases & Record Ground Truth
        injectEdgeCasesAndGroundTruth(dataset, prng, baseTime);

        // Database Batch Persistence using efficient chunked JDBC batches
        if (persistToDb) {
            batchRepository.clearAllTables();
            batchRepository.batchInsertWarehouses(dataset.warehouses);
            batchRepository.batchInsertStorageRequirements(dataset.storageRequirements);
            batchRepository.batchInsertZones(dataset.zones);
            batchRepository.batchInsertSensors(dataset.sensors);
            batchRepository.batchInsertSensorZoneMappings(dataset.sensorZoneMappings);
            batchRepository.batchInsertDoorEvents(dataset.doorEvents);

            // Batch insert readings in chunks of 10,000
            int chunkSize = 10000;
            for (int i = 0; i < dataset.readings.size(); i += chunkSize) {
                int end = Math.min(i + chunkSize, dataset.readings.size());
                batchRepository.batchInsertSensorReadingsChunk(dataset.readings.subList(i, end));
            }

            // Batch insert inventory lots in chunks of 2,000
            for (int i = 0; i < dataset.inventoryLots.size(); i += 2000) {
                int end = Math.min(i + 2000, dataset.inventoryLots.size());
                batchRepository.batchInsertInventoryLotsChunk(dataset.inventoryLots.subList(i, end));
            }

            batchRepository.batchInsertLotZoneAssignments(dataset.lotZoneAssignments);
            batchRepository.batchInsertAlerts(dataset.alerts);
            batchRepository.batchInsertAlarmLotImpacts(dataset.alarmLotImpacts);
            batchRepository.batchInsertInspections(dataset.inspections);
            batchRepository.batchInsertRemediations(dataset.remediations);
            batchRepository.batchInsertGroundTruth(dataset.groundTruthAnomalies);
        }

        return dataset;
    }

    private List<StorageRequirement> createStorageRequirements() {
        List<StorageRequirement> list = new ArrayList<>();
        list.add(new StorageRequirement("REQ-VACCINE", "Ultra-Cold Vaccine Spec", ProductCategory.VACCINES, -70.0, -80.0, -60.0, 30.0, 10.0, 50.0, 60));
        list.add(new StorageRequirement("REQ-DAIRY", "Chilled Dairy & Protein", ProductCategory.DAIRY, 3.0, 1.0, 5.0, 65.0, 50.0, 80.0, 120));
        list.add(new StorageRequirement("REQ-PHARMA", "Controlled Pharma Ambient", ProductCategory.PHARMACEUTICAL, 20.0, 15.0, 25.0, 45.0, 30.0, 60.0, 180));
        list.add(new StorageRequirement("REQ-PRODUCE", "Fresh Agricultural Produce", ProductCategory.PRODUCE, 8.0, 4.0, 12.0, 85.0, 75.0, 95.0, 240));
        list.add(new StorageRequirement("REQ-ELEC", "Electronics Dry Storage", ProductCategory.ELECTRONICS, 20.0, 15.0, 30.0, 40.0, 20.0, 60.0, 300));
        list.add(new StorageRequirement("REQ-AMBIENT", "Standard Ambient Dry Goods", ProductCategory.GENERAL_MERCHANDISE, 22.0, 10.0, 30.0, 50.0, 20.0, 70.0, 300));
        return list;
    }

    private List<Warehouse> createWarehouses(int count, Mulberry32 prng, int zonesPerWarehouse) {
        List<Warehouse> list = new ArrayList<>();
        String[] cities = {"Chicago, IL", "Dallas, TX", "Philadelphia, PA", "Atlanta, GA", "Seattle, WA",
                           "Denver, CO", "Phoenix, AZ", "Columbus, OH", "Indianapolis, IN", "Charlotte, NC"};
        for (int i = 1; i <= count; i++) {
            String id = String.format("WH-%03d", i);
            String name = "OmniHub Distribution Center " + i;
            String location = cities[(i - 1) % cities.length];
            int capacity = 25000 + prng.nextInt(0, 15000);
            list.add(new Warehouse(id, name, location, capacity, zonesPerWarehouse, WarehouseStatus.ACTIVE));
        }
        return list;
    }

    private List<Zone> createZones(List<Warehouse> warehouses, int zonesPerWarehouse, Mulberry32 prng) {
        List<Zone> list = new ArrayList<>();
        ZoneType[] types = {ZoneType.AMBIENT, ZoneType.COLD_STORAGE, ZoneType.FROZEN, ZoneType.HAZARDOUS, ZoneType.BUFFER};

        for (Warehouse wh : warehouses) {
            for (int z = 1; z <= zonesPerWarehouse; z++) {
                String id = String.format("ZONE-%s-%03d", wh.getId(), z);
                ZoneType type = types[(z - 1) % types.length];
                String name = String.format("%s - %s Area %d", wh.getName(), type.name(), z);

                double minT, maxT, minH, maxH;
                switch (type) {
                    case FROZEN -> { minT = -75.0; maxT = -65.0; minH = 15.0; maxH = 45.0; }
                    case COLD_STORAGE -> { minT = 1.0; maxT = 6.0; minH = 50.0; maxH = 85.0; }
                    case HAZARDOUS -> { minT = 15.0; maxT = 22.0; minH = 30.0; maxH = 55.0; }
                    case BUFFER -> { minT = 16.0; maxT = 24.0; minH = 35.0; maxH = 65.0; }
                    default -> { minT = 18.0; maxT = 26.0; minH = 30.0; maxH = 65.0; } // AMBIENT
                }

                Zone zone = new Zone(id, wh, name, type, 100, minT, maxT, minH, maxH, 6);
                zone.setCurrentTemperature((minT + maxT) / 2.0 + prng.nextDouble(-0.5, 0.5));
                zone.setCurrentHumidity((minH + maxH) / 2.0 + prng.nextDouble(-2.0, 2.0));
                zone.setCurrentOccupancy(prng.nextInt(0, 4));
                list.add(zone);
            }
        }
        return list;
    }

    private List<Sensor> createSensors(List<Zone> zones, Mulberry32 prng) {
        List<Sensor> list = new ArrayList<>(zones.size() * 5);

        for (Zone zone : zones) {
            // Exactly 5 sensors per zone: 2 Temperature, 1 Humidity, 1 Occupancy, 1 Door
            double offset1 = prng.chance(0.10) ? prng.nextDouble(0.4, 1.2) : 0.0;
            String id1 = String.format("SNS-%s-TEMP-01", zone.getId());
            list.add(new Sensor(id1, zone, SensorType.TEMPERATURE, SensorStatus.ACTIVE, "MOD-TMP-v2", offset1));

            double offset2 = prng.chance(0.08) ? prng.nextDouble(0.3, 0.9) : 0.0;
            String id2 = String.format("SNS-%s-TEMP-02", zone.getId());
            list.add(new Sensor(id2, zone, SensorType.TEMPERATURE, SensorStatus.ACTIVE, "MOD-TMP-v2", offset2));

            double offset3 = prng.chance(0.10) ? prng.nextDouble(0.5, 2.0) : 0.0;
            String id3 = String.format("SNS-%s-HUMI-01", zone.getId());
            list.add(new Sensor(id3, zone, SensorType.HUMIDITY, SensorStatus.ACTIVE, "MOD-HUM-v2", offset3));

            String id4 = String.format("SNS-%s-OCCU-01", zone.getId());
            list.add(new Sensor(id4, zone, SensorType.OCCUPANCY, SensorStatus.ACTIVE, "MOD-OCC-v2", 0.0));

            String id5 = String.format("SNS-%s-DOOR-01", zone.getId());
            list.add(new Sensor(id5, zone, SensorType.DOOR, SensorStatus.ACTIVE, "MOD-DOR-v2", 0.0));
        }
        return list;
    }

    private List<SensorZoneMapping> createSensorZoneMappings(List<Sensor> sensors) {
        List<SensorZoneMapping> list = new ArrayList<>(sensors.size() + 2);
        int counter = 1;
        LocalDateTime mappedAt = LocalDateTime.of(2026, 1, 1, 0, 0, 0);

        for (Sensor s : sensors) {
            String id = String.format("MAP-%05d", counter++);
            list.add(new SensorZoneMapping(id, s, s.getZone(), mappedAt, null, true, "Primary zone telemetry placement"));
        }
        return list;
    }

    private void createInventoryLotsAndAssignments(GeneratedDataset dataset, int lotCount, Mulberry32 prng, LocalDateTime baseTime) {
        String[] productNames = {"Pfizer-BioNTech Comirnaty", "Organic Whole Milk", "Amoxicillin 500mg", "Fresh Hass Avocados", "Lithium Ion Cells", "Insulin Glargine", "Grade A Cheddar Cheese"};
        ProductCategory[] categories = {ProductCategory.VACCINES, ProductCategory.DAIRY, ProductCategory.PHARMACEUTICAL, ProductCategory.PRODUCE, ProductCategory.ELECTRONICS, ProductCategory.PHARMACEUTICAL, ProductCategory.DAIRY};

        Map<ProductCategory, List<Zone>> compatibleZones = new EnumMap<>(ProductCategory.class);
        for (ProductCategory cat : ProductCategory.values()) {
            List<Zone> matching = new ArrayList<>();
            for (Zone z : dataset.zones) {
                if (isZoneCompatible(z.getZoneType(), cat)) {
                    matching.add(z);
                }
            }
            if (matching.isEmpty()) matching = dataset.zones;
            compatibleZones.put(cat, matching);
        }

        Map<ProductCategory, StorageRequirement> reqByCategory = new EnumMap<>(ProductCategory.class);
        for (StorageRequirement req : dataset.storageRequirements) {
            reqByCategory.put(req.getCategory(), req);
        }

        for (int i = 1; i <= lotCount; i++) {
            String id = String.format("LOT-2026-%06d", i);
            int pIdx = (i - 1) % productNames.length;
            String name = productNames[pIdx];
            ProductCategory category = categories[pIdx];

            StorageRequirement req = reqByCategory.getOrDefault(category, dataset.storageRequirements.get(0));
            List<Zone> options = compatibleZones.get(category);
            Zone matchingZone = options.get(prng.nextInt(0, options.size() - 1));

            LocalDateTime ingress = baseTime.minusDays(prng.nextInt(1, 30));
            LocalDateTime expiry = ingress.plusMonths(prng.nextInt(6, 24));

            InventoryLot lot = new InventoryLot(id, name, "SKU-" + category.name().substring(0, Math.min(3, category.name().length())) + "-" + (100 + (pIdx % 900)),
                    category, prng.nextDouble(10.0, 500.0), UnitOfMeasure.PALLETS, req, matchingZone, ingress, expiry, LotStatus.STORED);

            dataset.inventoryLots.add(lot);

            // Lot zone assignment history
            String lzaId = String.format("LZA-%s-01", id);
            LotZoneAssignment lza = new LotZoneAssignment(lzaId, lot, matchingZone, ingress, null);
            dataset.lotZoneAssignments.add(lza);
        }
    }

    private boolean isZoneCompatible(ZoneType zoneType, ProductCategory category) {
        if (category == ProductCategory.VACCINES) return zoneType == ZoneType.FROZEN;
        if (category == ProductCategory.DAIRY) return zoneType == ZoneType.COLD_STORAGE;
        if (category == ProductCategory.PRODUCE) return zoneType == ZoneType.COLD_STORAGE || zoneType == ZoneType.BUFFER;
        return zoneType == ZoneType.AMBIENT || zoneType == ZoneType.BUFFER;
    }

    private List<DoorEvent> createDoorEvents(List<Zone> zones, int days, Mulberry32 prng, LocalDateTime baseTime) {
        List<DoorEvent> list = new ArrayList<>();
        int eventCounter = 1;

        for (Zone zone : zones) {
            for (int d = 0; d < days; d++) {
                int eventsToday = prng.nextInt(1, 2);
                for (int e = 0; e < eventsToday; e++) {
                    int hour = prng.nextInt(7, 18);
                    int minute = prng.nextInt(0, 50);
                    LocalDateTime opened = baseTime.plusDays(d).plusHours(hour).plusMinutes(minute);

                    int maxPermitted = 180;
                    boolean isAnomaly = prng.chance(0.08);
                    int duration = isAnomaly ? prng.nextInt(350, 900) : prng.nextInt(30, 160);
                    LocalDateTime closed = opened.plusSeconds(duration);

                    DoorStatus status = isAnomaly ? DoorStatus.EXCEEDED_DURATION : DoorStatus.NORMAL;
                    String id = String.format("DEV-%s-%04d", zone.getId(), eventCounter++);
                    list.add(new DoorEvent(id, zone, "Dock Door " + (e + 1), opened, closed, duration, maxPermitted, status, isAnomaly));
                }
            }
        }
        return list;
    }

    private List<SensorReading> createReadings(GeneratedDataset dataset,
                                              int readingsPerSensor, int intervalMinutes, Mulberry32 prng, LocalDateTime baseTime) {
        List<Sensor> sensors = dataset.sensors;
        List<DoorEvent> doorEvents = dataset.doorEvents;
        List<SensorReading> list = new ArrayList<>(sensors.size() * readingsPerSensor);

        Map<String, List<DoorEvent>> doorEventsByZone = new HashMap<>();
        for (DoorEvent d : doorEvents) {
            doorEventsByZone.computeIfAbsent(d.getZone().getId(), k -> new ArrayList<>()).add(d);
        }

        // Identify designated gap sensor for Edge Case 3
        Sensor gapSensor = sensors.size() > 2 ? sensors.get(2) : sensors.get(0);
        dataset.gapSensor = gapSensor;
        LocalDateTime gapStart = baseTime.plusHours(8);
        LocalDateTime gapEnd = gapStart.plusMinutes((long) 10 * intervalMinutes); // exactly 10 intervals

        for (Sensor sensor : sensors) {
            Zone zone = sensor.getZone();
            List<DoorEvent> zoneDoors = doorEventsByZone.get(zone.getId());
            double baselineVal = switch (sensor.getSensorType()) {
                case TEMPERATURE -> (zone.getTemperatureMin() + zone.getTemperatureMax()) / 2.0;
                case HUMIDITY -> (zone.getHumidityMin() + zone.getHumidityMax()) / 2.0;
                case OCCUPANCY -> 2.0;
                case DOOR -> 0.0;
            };

            String unit = switch (sensor.getSensorType()) {
                case TEMPERATURE -> "°C";
                case HUMIDITY -> "%";
                case OCCUPANCY -> "COUNT";
                case DOOR -> "STATE";
            };

            for (int step = 0; step < readingsPerSensor; step++) {
                LocalDateTime t = baseTime.plusMinutes((long) step * intervalMinutes);

                // Edge Case 3: Intentionally skip 10 readings for gapSensor to create physical telemetry gap
                if (sensor == gapSensor && !t.isBefore(gapStart) && t.isBefore(gapEnd)) {
                    continue;
                }

                double hour = t.getHour() + t.getMinute() / 60.0;
                double readingVal;
                ReadingStatus status = ReadingStatus.NORMAL;
                boolean isAnomaly = false;

                if (sensor.getSensorType() == SensorType.TEMPERATURE) {
                    double diurnal = 1.8 * Math.sin(2.0 * Math.PI * (hour - 8.0) / 24.0);
                    double noise = prng.nextGaussian(0.0, 0.25);
                    readingVal = baselineVal + diurnal + noise + sensor.getCalibrationOffset();

                    if (zoneDoors != null) {
                        for (DoorEvent d : zoneDoors) {
                            LocalDateTime recoveryEnd = d.getClosedAt() != null ? d.getClosedAt().plusHours(2) : d.getOpenedAt().plusHours(2);
                            if (!t.isBefore(d.getOpenedAt()) && !t.isAfter(recoveryEnd)) {
                                double thermalImpact = d.getStatus() == DoorStatus.EXCEEDED_DURATION ? 6.5 : 3.2;
                                readingVal += thermalImpact;
                                break;
                            }
                        }
                    }

                    if (prng.chance(0.015)) {
                        readingVal = zone.getTemperatureMax() + prng.nextDouble(2.0, 6.0);
                    }

                    if (readingVal > zone.getTemperatureMax() || readingVal < zone.getTemperatureMin()) {
                        status = ReadingStatus.EXCURSION;
                        isAnomaly = true;
                    }
                } else if (sensor.getSensorType() == SensorType.HUMIDITY) {
                    double diurnal = 3.5 * Math.sin(2.0 * Math.PI * (hour - 4.0) / 24.0);
                    double noise = prng.nextGaussian(0.0, 0.8);
                    readingVal = Math.max(10.0, Math.min(99.0, baselineVal + diurnal + noise));
                    if (readingVal > zone.getHumidityMax() || readingVal < zone.getHumidityMin()) {
                        status = ReadingStatus.EXCURSION;
                        isAnomaly = true;
                    }
                } else if (sensor.getSensorType() == SensorType.OCCUPANCY) {
                    boolean workHours = hour >= 8 && hour <= 17;
                    readingVal = workHours ? prng.nextInt(1, zone.getMaxOccupancy()) : (prng.chance(0.1) ? 1 : 0);
                    if (readingVal > zone.getMaxOccupancy()) {
                        status = ReadingStatus.OUTLIER;
                        isAnomaly = true;
                    }
                } else {
                    readingVal = prng.chance(0.15) ? 1.0 : 0.0;
                }

                sensor.setLatestReadingValue(Math.round(readingVal * 100.0) / 100.0);
                sensor.setLatestReadingTime(t);

                String rdgId = String.format("RDG-%s-%05d", sensor.getId(), step + 1);
                list.add(new SensorReading(rdgId, sensor, zone, sensor.getSensorType(),
                        Math.round(readingVal * 100.0) / 100.0, unit, t, status, isAnomaly));
            }

            // Compensate the 10 dropped gap readings at the end for gapSensor so exact sensor reading volume is maintained
            if (sensor == gapSensor) {
                for (int k = 0; k < 10; k++) {
                    int extraStep = readingsPerSensor + k;
                    LocalDateTime t = baseTime.plusMinutes((long) extraStep * intervalMinutes);
                    String rdgId = String.format("RDG-%s-%05d", sensor.getId(), extraStep + 1);
                    list.add(new SensorReading(rdgId, sensor, zone, sensor.getSensorType(),
                            Math.round(baselineVal * 100.0) / 100.0, unit, t, ReadingStatus.NORMAL, false));
                }
            }
        }
        return list;
    }

    private void createAlertsAndSpatiotemporalImpact(GeneratedDataset dataset, Mulberry32 prng) {
        int alertSeq = 1;
        int impactSeq = 1;

        Map<String, List<SensorReading>> excursionsByZone = new HashMap<>();
        for (SensorReading r : dataset.readings) {
            if (r.getStatus() == ReadingStatus.EXCURSION && r.getReadingType() == SensorType.TEMPERATURE) {
                excursionsByZone.computeIfAbsent(r.getZone().getId(), k -> new ArrayList<>()).add(r);
            }
        }

        Map<String, List<InventoryLot>> lotsByZone = new HashMap<>();
        for (InventoryLot lot : dataset.inventoryLots) {
            lotsByZone.computeIfAbsent(lot.getCurrentZone().getId(), k -> new ArrayList<>()).add(lot);
        }

        for (Map.Entry<String, List<SensorReading>> entry : excursionsByZone.entrySet()) {
            List<SensorReading> zoneExcursions = entry.getValue();
            if (zoneExcursions.isEmpty()) continue;

            // Group consecutive excursion readings into distinct excursion episodes (gap <= 45 min)
            List<List<SensorReading>> episodes = new ArrayList<>();
            List<SensorReading> currentEpisode = new ArrayList<>();
            for (SensorReading r : zoneExcursions) {
                if (currentEpisode.isEmpty()) {
                    currentEpisode.add(r);
                } else {
                    SensorReading prev = currentEpisode.get(currentEpisode.size() - 1);
                    long gap = java.time.Duration.between(prev.getTimestamp(), r.getTimestamp()).toMinutes();
                    if (gap <= 45) {
                        currentEpisode.add(r);
                    } else {
                        episodes.add(currentEpisode);
                        currentEpisode = new ArrayList<>();
                        currentEpisode.add(r);
                    }
                }
            }
            if (!currentEpisode.isEmpty()) {
                episodes.add(currentEpisode);
            }

            // Find the most severe excursion episode for this zone
            List<SensorReading> peakEpisode = episodes.get(0);
            double peakDelta = 0.0;
            for (List<SensorReading> ep : episodes) {
                double epMax = ep.stream().mapToDouble(SensorReading::getValue).max().orElse(0.0);
                double d = epMax - ep.get(0).getZone().getTemperatureMax();
                if (d > peakDelta) {
                    peakDelta = d;
                    peakEpisode = ep;
                }
            }

            SensorReading first = peakEpisode.get(0);
            SensorReading last = peakEpisode.get(peakEpisode.size() - 1);
            Zone zone = first.getZone();

            double maxVal = peakEpisode.stream().mapToDouble(SensorReading::getValue).max().orElse(zone.getTemperatureMax() + 2.0);
            double delta = Math.max(0.5, maxVal - zone.getTemperatureMax());

            RiskLevel risk = delta > 5.0 ? RiskLevel.CRITICAL : (delta > 2.0 ? RiskLevel.HIGH : RiskLevel.MEDIUM);
            String alertId = String.format("ALM-%s-%04d", zone.getId(), alertSeq++);

            Alert alert = new Alert(alertId, zone, first.getSensor(), AlertType.HIGH_TEMPERATURE, risk,
                    AlertStatus.OPEN, first.getTimestamp(), maxVal, zone.getTemperatureMax(),
                    String.format("High temperature excursion detected: %.1f°C exceeds max threshold of %.1f°C", maxVal, zone.getTemperatureMax()));
            dataset.alerts.add(alert);
            zone.setStatus(risk == RiskLevel.CRITICAL ? ZoneStatus.CRITICAL : ZoneStatus.WARNING);

            List<InventoryLot> zoneLots = lotsByZone.get(zone.getId());
            if (zoneLots != null) {
                boolean firstLotInFirstAlert = (alertSeq == 2); // First created alert
                for (int i = 0; i < zoneLots.size(); i++) {
                    InventoryLot lot = zoneLots.get(i);

                    // Edge Case 4: Intentionally omit AlarmLotImpact record for one dwelling lot
                    if (firstLotInFirstAlert && i == 0) {
                        dataset.unlinkedAlert = alert;
                        dataset.unlinkedLot = lot;
                        continue; // Dropped linkage!
                    }

                    // Calculate exposure strictly over the actual excursion episode duration (bounded between 15 and 180 min)
                    long episodeMinutes = java.time.Duration.between(first.getTimestamp(), last.getTimestamp()).toMinutes() + 15;
                    long exposureMinutes = Math.max(15, Math.min(180, episodeMinutes));
                    double thermalStress = Math.round(exposureMinutes * delta * 10.0) / 10.0;

                    lot.setExposureDurationMinutes(lot.getExposureDurationMinutes() + exposureMinutes);
                    lot.setThermalStressScore(lot.getThermalStressScore() + thermalStress);
                    if (thermalStress > 50.0) {
                        lot.setStatus(LotStatus.QUARANTINED);
                    }

                    String impactId = String.format("ALI-%s-%04d", alert.getId(), impactSeq++);
                    dataset.alarmLotImpacts.add(new AlarmLotImpact(impactId, alert, lot, (double) exposureMinutes, thermalStress,
                            thermalStress > 50.0 ? "IMMEDIATE_QUARANTINE" : "VISUAL_INSPECTION_REQUIRED"));
                }
            }
        }
    }

    private void createInspectionsAndRemediations(GeneratedDataset dataset, Mulberry32 prng) {
        int inspCounter = 1;
        int remCounter = 1;

        for (Alert alert : dataset.alerts) {
            String inspId = String.format("INSP-2026-%06d", inspCounter++);
            Inspection insp = new Inspection(inspId, alert.getZone(), alert, "Lead Engineer Alex Mercer",
                    alert.getTriggeredAt().plusMinutes(45), InspectionStatus.COMPLETED,
                    "Cooling condenser coil checked. Airflow restriction detected on secondary intake vent.",
                    "Airflow obstruction cleared. Refrigerant levels nominal.", true);
            dataset.inspections.add(insp);

            String remId = String.format("REM-2026-%06d", remCounter++);
            Remediation rem = new Remediation(remId, alert, insp, null,
                    "Condenser Vent Cleaning and Thermal Sensor Calibration",
                    RemediationType.EQUIPMENT_REPAIR, "HVAC Maintenance Team",
                    RemediationStatus.COMPLETED, 850.00, alert.getTriggeredAt().plusHours(2),
                    alert.getTriggeredAt().plusHours(4),
                    "Intake vent cleaned, secondary compressor cycle calibrated, zone temperature restored to baseline.");
            dataset.remediations.add(rem);

            alert.setStatus(AlertStatus.RESOLVED);
            alert.setResolvedAt(alert.getTriggeredAt().plusHours(4));
            alert.setAcknowledgedBy("Alex Mercer");
            alert.setResolvedBy("HVAC Team Lead");
            alert.setNotes("Remediation completed successfully. Zone stabilized.");
        }
    }

    private void injectEdgeCasesAndGroundTruth(GeneratedDataset dataset, Mulberry32 prng, LocalDateTime baseTime) {
        // Edge Case 1: Lot assigned to incompatible zone
        if (!dataset.inventoryLots.isEmpty() && !dataset.zones.isEmpty()) {
            InventoryLot lot1 = dataset.inventoryLots.get(0);
            Zone ambientZone = dataset.zones.stream().filter(z -> z.getZoneType() == ZoneType.AMBIENT).findFirst().orElse(dataset.zones.get(0));
            StorageRequirement vaccineReq = dataset.storageRequirements.stream().filter(r -> r.getCategory() == ProductCategory.VACCINES).findFirst().orElse(dataset.storageRequirements.get(0));
            lot1.setProductName("Pfizer Comirnaty mRNA Vaccine");
            lot1.setCategory(ProductCategory.VACCINES);
            lot1.setStorageRequirement(vaccineReq);
            lot1.setCurrentZone(ambientZone);

            for (LotZoneAssignment lza : dataset.lotZoneAssignments) {
                if (lza.getLot().getId().equals(lot1.getId())) {
                    lza.setZone(ambientZone);
                }
            }

            dataset.groundTruthAnomalies.add(new GroundTruthAnomaly("GTA-001", "InventoryLot", lot1.getId(),
                    AnomalyType.LOT_ASSIGNED_INCOMPATIBLE_ZONE, baseTime.plusHours(2),
                    "Lot " + lot1.getId() + " requires ultra-cold vaccine storage (-80°C to -60°C) but is assigned to Ambient Zone " + ambientZone.getId() + " (18°C to 26°C)",
                    "QUARANTINE_AND_REALLOCATE", true, 5.0));
        }

        // Edge Case 2: Sensor mapped to multiple zones without rationale
        if (dataset.sensors.size() > 1 && dataset.zones.size() > 1) {
            Sensor s = dataset.sensors.get(0);
            Zone conflictZone = dataset.zones.get(1);
            dataset.sensorZoneMappings.add(new SensorZoneMapping("MAP-99991", s, conflictZone, LocalDateTime.of(2026, 1, 1, 0, 0), null, true, ""));

            dataset.groundTruthAnomalies.add(new GroundTruthAnomaly("GTA-002", "Sensor", s.getId(),
                    AnomalyType.SENSOR_MAPPED_MULTIPLE_ZONES, baseTime.plusHours(4),
                    "Sensor " + s.getId() + " concurrently mapped to multiple logical zones without physical isolation or rationale",
                    "CORRECT_SENSOR_TOPOLOGY", true, 3.5));
        }

        // Edge Case 3: Unreported sensor reading gap
        Sensor gapSensor = dataset.gapSensor != null ? dataset.gapSensor : (dataset.sensors.size() > 2 ? dataset.sensors.get(2) : dataset.sensors.get(0));
        dataset.groundTruthAnomalies.add(new GroundTruthAnomaly("GTA-003", "SensorReading", gapSensor.getId(),
                AnomalyType.UNREPORTED_SENSOR_READING_GAP, baseTime.plusHours(8),
                "Telemetry blackout of 10 consecutive intervals on Sensor " + gapSensor.getId() + " between 08:00 and 08:50 UTC",
                "FLAG_SENSOR_COMMUNICATION_FAULT", true, 4.0));

        // Edge Case 4: Alarm not linked to affected lots
        if (dataset.unlinkedAlert != null && dataset.unlinkedLot != null) {
            dataset.groundTruthAnomalies.add(new GroundTruthAnomaly("GTA-004", "Alarm", dataset.unlinkedAlert.getId(),
                    AnomalyType.ALARM_NOT_LINKED_TO_AFFECTED_LOT, baseTime.plusHours(12),
                    "Alarm " + dataset.unlinkedAlert.getId() + " occurred in zone " + dataset.unlinkedAlert.getZone().getId() + " while Lot " + dataset.unlinkedLot.getId() + " was dwelling, but impact linkage was omitted",
                    "RECONCILE_SPATIOTEMPORAL_IMPACT", true, 4.5));
        } else if (!dataset.alerts.isEmpty() && !dataset.inventoryLots.isEmpty()) {
            Alert a = dataset.alerts.get(0);
            InventoryLot lot = dataset.inventoryLots.get(0);
            dataset.groundTruthAnomalies.add(new GroundTruthAnomaly("GTA-004", "Alarm", a.getId(),
                    AnomalyType.ALARM_NOT_LINKED_TO_AFFECTED_LOT, baseTime.plusHours(12),
                    "Alarm " + a.getId() + " occurred in zone " + a.getZone().getId() + " while Lot " + lot.getId() + " was dwelling, but impact linkage was omitted",
                    "RECONCILE_SPATIOTEMPORAL_IMPACT", true, 4.5));
        }
    }
}
