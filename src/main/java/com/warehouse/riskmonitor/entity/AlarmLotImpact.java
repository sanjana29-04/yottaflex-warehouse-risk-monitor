package com.warehouse.riskmonitor.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "alarm_lot_impacts", indexes = {
    @Index(name = "idx_ali_alert", columnList = "alert_id"),
    @Index(name = "idx_ali_lot", columnList = "lot_id")
})
public class AlarmLotImpact {

    @Id
    @Column(name = "id", length = 64, nullable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alert_id", nullable = false, foreignKey = @ForeignKey(name = "fk_ali_alert"))
    private Alert alert;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lot_id", nullable = false, foreignKey = @ForeignKey(name = "fk_ali_lot"))
    private InventoryLot lot;

    @Column(name = "exposure_minutes", nullable = false)
    private Double exposureMinutes = 0.0;

    @Column(name = "thermal_stress_score", nullable = false)
    private Double thermalStressScore = 0.0;

    @Column(name = "action_recommended", length = 128)
    private String actionRecommended;

    public AlarmLotImpact() {}

    public AlarmLotImpact(String id, Alert alert, InventoryLot lot, Double exposureMinutes,
                          Double thermalStressScore, String actionRecommended) {
        this.id = id;
        this.alert = alert;
        this.lot = lot;
        this.exposureMinutes = exposureMinutes != null ? exposureMinutes : 0.0;
        this.thermalStressScore = thermalStressScore != null ? thermalStressScore : 0.0;
        this.actionRecommended = actionRecommended;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public Alert getAlert() { return alert; }
    public void setAlert(Alert alert) { this.alert = alert; }

    public String getAlertId() { return alert != null ? alert.getId() : null; }

    public InventoryLot getLot() { return lot; }
    public void setLot(InventoryLot lot) { this.lot = lot; }

    public String getLotId() { return lot != null ? lot.getId() : null; }

    public Double getExposureMinutes() { return exposureMinutes; }
    public void setExposureMinutes(Double exposureMinutes) { this.exposureMinutes = exposureMinutes; }

    public Double getThermalStressScore() { return thermalStressScore; }
    public void setThermalStressScore(Double thermalStressScore) { this.thermalStressScore = thermalStressScore; }

    public String getActionRecommended() { return actionRecommended; }
    public void setActionRecommended(String actionRecommended) { this.actionRecommended = actionRecommended; }
}

