package com.warehouse.riskmonitor.service;

import com.warehouse.riskmonitor.entity.Alert;
import com.warehouse.riskmonitor.entity.DoorEvent;
import com.warehouse.riskmonitor.entity.Zone;
import com.warehouse.riskmonitor.entity.enums.AlertStatus;
import com.warehouse.riskmonitor.entity.enums.AlertType;
import com.warehouse.riskmonitor.entity.enums.DoorStatus;
import com.warehouse.riskmonitor.entity.enums.RiskLevel;
import com.warehouse.riskmonitor.repository.AlertRepository;
import com.warehouse.riskmonitor.repository.DoorEventRepository;
import com.warehouse.riskmonitor.repository.ZoneRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
public class DoorEventService {

    private final DoorEventRepository doorEventRepository;
    private final ZoneRepository zoneRepository;
    private final AlertRepository alertRepository;

    public DoorEventService(DoorEventRepository doorEventRepository,
                            ZoneRepository zoneRepository,
                            AlertRepository alertRepository) {
        this.doorEventRepository = doorEventRepository;
        this.zoneRepository = zoneRepository;
        this.alertRepository = alertRepository;
    }

    public Page<DoorEvent> getDoorEventsPaged(String zoneId, DoorStatus status, Pageable pageable) {
        return doorEventRepository.findFiltered(zoneId, status, pageable);
    }

    @Transactional
    public DoorEvent recordDoorEvent(String zoneId, String doorName, LocalDateTime openedAt, LocalDateTime closedAt, int maxPermittedSeconds) {
        Zone zone = zoneRepository.findById(zoneId)
                .orElseThrow(() -> new IllegalArgumentException("Zone not found: " + zoneId));

        if (openedAt == null) openedAt = LocalDateTime.now();

        Integer duration = null;
        DoorStatus status = DoorStatus.NORMAL;
        boolean triggerAlert = false;

        if (closedAt != null) {
            duration = (int) Duration.between(openedAt, closedAt).toSeconds();
            if (duration > maxPermittedSeconds) {
                status = DoorStatus.EXCEEDED_DURATION;
                triggerAlert = true;
            }
        }

        long count = doorEventRepository.count() + 1;
        String id = String.format("DEV-%s-%04d", zoneId, count);
        DoorEvent event = new DoorEvent(id, zone, doorName, openedAt, closedAt, duration, maxPermittedSeconds, status, triggerAlert);
        doorEventRepository.save(event);

        if (triggerAlert) {
            String alertId = String.format("ALM-%s-%04d", zoneId, count);
            Alert alert = new Alert(alertId, zone, null, AlertType.DOOR_OPEN_PROLONGED, RiskLevel.HIGH,
                    AlertStatus.OPEN, openedAt, (double) duration, (double) maxPermittedSeconds,
                    String.format("Door '%s' remained open for %ds (permitted: %ds)", doorName, duration, maxPermittedSeconds));
            alertRepository.save(alert);
        }

        return event;
    }

    public long getCountByStatus(DoorStatus status) {
        return doorEventRepository.countByStatus(status);
    }
}

