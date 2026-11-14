package com.warehouse.riskmonitor.entity;

import com.warehouse.riskmonitor.entity.enums.DoorStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "door_events", indexes = {
    @Index(name = "idx_door_zone", columnList = "zone_id"),
    @Index(name = "idx_door_opened_at", columnList = "opened_at"),
    @Index(name = "idx_door_status", columnList = "status")
})
public class DoorEvent {

    @Id
    @Column(name = "id", length = 64, nullable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "zone_id", nullable = false, foreignKey = @ForeignKey(name = "fk_door_zone"))
    private Zone zone;

    @Column(name = "door_name", length = 64, nullable = false)
    private String doorName;

    @Column(name = "opened_at", nullable = false)
    private LocalDateTime openedAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Column(name = "max_permitted_seconds", nullable = false)
    private Integer maxPermittedSeconds = 300;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 32, nullable = false)
    private DoorStatus status = DoorStatus.NORMAL;

    @Column(name = "alert_triggered", nullable = false)
    private Boolean alertTriggered = false;

    public DoorEvent() {}

    public DoorEvent(String id, Zone zone, String doorName, LocalDateTime openedAt, LocalDateTime closedAt,
                     Integer durationSeconds, Integer maxPermittedSeconds, DoorStatus status, Boolean alertTriggered) {
        this.id = id;
        this.zone = zone;
        this.doorName = doorName;
        this.openedAt = openedAt;
        this.closedAt = closedAt;
        this.durationSeconds = durationSeconds;
        this.maxPermittedSeconds = maxPermittedSeconds != null ? maxPermittedSeconds : 300;
        this.status = status != null ? status : DoorStatus.NORMAL;
        this.alertTriggered = alertTriggered != null ? alertTriggered : false;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public Zone getZone() { return zone; }
    public void setZone(Zone zone) { this.zone = zone; }

    public String getZoneId() {
        return zone != null ? zone.getId() : null;
    }

    public String getDoorName() { return doorName; }
    public void setDoorName(String doorName) { this.doorName = doorName; }

    public LocalDateTime getOpenedAt() { return openedAt; }
    public void setOpenedAt(LocalDateTime openedAt) { this.openedAt = openedAt; }

    public LocalDateTime getClosedAt() { return closedAt; }
    public void setClosedAt(LocalDateTime closedAt) { this.closedAt = closedAt; }

    public Integer getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(Integer durationSeconds) { this.durationSeconds = durationSeconds; }

    public Integer getMaxPermittedSeconds() { return maxPermittedSeconds; }
    public void setMaxPermittedSeconds(Integer maxPermittedSeconds) { this.maxDoorDuration(); }

    private void maxDoorDuration() {
        if (maxPermittedSeconds == null) maxPermittedSeconds = 300;
    }

    public DoorStatus getStatus() { return status; }
    public void setStatus(DoorStatus status) { this.status = status; }

    public Boolean getAlertTriggered() { return alertTriggered; }
    public void setAlertTriggered(Boolean alertTriggered) { this.alertTriggered = alertTriggered; }
}

