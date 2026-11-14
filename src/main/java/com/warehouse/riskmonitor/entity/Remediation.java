package com.warehouse.riskmonitor.entity;

import com.warehouse.riskmonitor.entity.enums.RemediationStatus;
import com.warehouse.riskmonitor.entity.enums.RemediationType;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "remediations", indexes = {
    @Index(name = "idx_rem_alert", columnList = "alert_id"),
    @Index(name = "idx_rem_status", columnList = "status"),
    @Index(name = "idx_rem_type", columnList = "action_type")
})
public class Remediation {

    @Id
    @Column(name = "id", length = 32, nullable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alert_id", nullable = false, foreignKey = @ForeignKey(name = "fk_remediation_alert"))
    private Alert alert;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inspection_id", foreignKey = @ForeignKey(name = "fk_remediation_inspection"))
    private Inspection inspection;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lot_id", foreignKey = @ForeignKey(name = "fk_remediation_lot"))
    private InventoryLot inventoryLot;

    @Column(name = "action_title", length = 128, nullable = false)
    private String actionTitle;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", length = 32, nullable = false)
    private RemediationType actionType;

    @Column(name = "assigned_to", length = 64, nullable = false)
    private String assignedTo;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 32, nullable = false)
    private RemediationStatus status = RemediationStatus.SCHEDULED;

    @Column(name = "cost_impact", nullable = false)
    private Double costImpact = 0.0;

    @Column(name = "scheduled_at", nullable = false)
    private LocalDateTime scheduledAt = LocalDateTime.now();

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "resolution_notes", length = 1024)
    private String resolutionNotes;

    public Remediation() {}

    public Remediation(String id, Alert alert, Inspection inspection, InventoryLot inventoryLot,
                       String actionTitle, RemediationType actionType, String assignedTo,
                       RemediationStatus status, Double costImpact, LocalDateTime scheduledAt,
                       LocalDateTime completedAt, String resolutionNotes) {
        this.id = id;
        this.alert = alert;
        this.inspection = inspection;
        this.inventoryLot = inventoryLot;
        this.actionTitle = actionTitle;
        this.actionType = actionType;
        this.assignedTo = assignedTo;
        this.status = status != null ? status : RemediationStatus.SCHEDULED;
        this.costImpact = costImpact != null ? costImpact : 0.0;
        this.scheduledAt = scheduledAt != null ? scheduledAt : LocalDateTime.now();
        this.completedAt = completedAt;
        this.resolutionNotes = resolutionNotes;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public Alert getAlert() { return alert; }
    public void setAlert(Alert alert) { this.alert = alert; }

    public String getAlertId() { return alert != null ? alert.getId() : null; }

    public Inspection getInspection() { return inspection; }
    public void setInspection(Inspection inspection) { this.inspection = inspection; }

    public String getInspectionId() { return inspection != null ? inspection.getId() : null; }

    public InventoryLot getInventoryLot() { return inventoryLot; }
    public void setInventoryLot(InventoryLot inventoryLot) { this.inventoryLot = inventoryLot; }

    public String getInventoryLotId() { return inventoryLot != null ? inventoryLot.getId() : null; }

    public String getActionTitle() { return actionTitle; }
    public void setActionTitle(String actionTitle) { this.actionTitle = actionTitle; }

    public RemediationType getActionType() { return actionType; }
    public void setActionType(RemediationType actionType) { this.actionType = actionType; }

    public String getAssignedTo() { return assignedTo; }
    public void setAssignedTo(String assignedTo) { this.assignedTo = assignedTo; }

    public RemediationStatus getStatus() { return status; }
    public void setStatus(RemediationStatus status) { this.status = status; }

    public Double getCostImpact() { return costImpact; }
    public void setCostImpact(Double costImpact) { this.costImpact = costImpact; }

    public LocalDateTime getScheduledAt() { return scheduledAt; }
    public void setScheduledAt(LocalDateTime scheduledAt) { this.scheduledAt = scheduledAt; }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }

    public String getResolutionNotes() { return resolutionNotes; }
    public void setResolutionNotes(String resolutionNotes) { this.resolutionNotes = resolutionNotes; }
}

