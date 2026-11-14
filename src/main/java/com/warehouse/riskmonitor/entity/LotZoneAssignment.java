package com.warehouse.riskmonitor.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "lot_zone_assignments", indexes = {
    @Index(name = "idx_lza_lot", columnList = "lot_id"),
    @Index(name = "idx_lza_zone", columnList = "zone_id"),
    @Index(name = "idx_lza_ingress", columnList = "ingress_timestamp")
})
public class LotZoneAssignment {

    @Id
    @Column(name = "id", length = 64, nullable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lot_id", nullable = false, foreignKey = @ForeignKey(name = "fk_lza_lot"))
    private InventoryLot lot;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "zone_id", nullable = false, foreignKey = @ForeignKey(name = "fk_lza_zone"))
    private Zone zone;

    @Column(name = "ingress_timestamp", nullable = false)
    private LocalDateTime ingressTimestamp;

    @Column(name = "egress_timestamp")
    private LocalDateTime egressTimestamp;

    @Column(name = "dwell_minutes")
    private Long dwellMinutes;

    public LotZoneAssignment() {}

    public LotZoneAssignment(String id, InventoryLot lot, Zone zone, LocalDateTime ingressTimestamp, LocalDateTime egressTimestamp) {
        this.id = id;
        this.lot = lot;
        this.zone = zone;
        this.ingressTimestamp = ingressTimestamp;
        this.egressTimestamp = egressTimestamp;
        if (ingressTimestamp != null && egressTimestamp != null) {
            this.dwellMinutes = java.time.Duration.between(ingressTimestamp, egressTimestamp).toMinutes();
        }
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public InventoryLot getLot() { return lot; }
    public void setLot(InventoryLot lot) { this.lot = lot; }

    public String getLotId() { return lot != null ? lot.getId() : null; }

    public Zone getZone() { return zone; }
    public void setZone(Zone zone) { this.zone = zone; }

    public String getZoneId() { return zone != null ? zone.getId() : null; }

    public LocalDateTime getIngressTimestamp() { return ingressTimestamp; }
    public void setIngressTimestamp(LocalDateTime ingressTimestamp) { this.ingressTimestamp = ingressTimestamp; }

    public LocalDateTime getEgressTimestamp() { return egressTimestamp; }
    public void setEgressTimestamp(LocalDateTime egressTimestamp) {
        this.egressTimestamp = egressTimestamp;
        if (this.ingressTimestamp != null && egressTimestamp != null) {
            this.dwellMinutes = java.time.Duration.between(this.ingressTimestamp, egressTimestamp).toMinutes();
        }
    }

    public Long getDwellMinutes() { return dwellMinutes; }
    public void setDwellMinutes(Long dwellMinutes) { this.dwellMinutes = dwellMinutes; }
}

