package com.warehouse.riskmonitor.entity;

import com.warehouse.riskmonitor.entity.enums.InspectionStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "inspections", indexes = {
    @Index(name = "idx_insp_zone", columnList = "zone_id"),
    @Index(name = "idx_insp_status", columnList = "status"),
    @Index(name = "idx_insp_date", columnList = "inspection_date")
})
public class Inspection {

    @Id
    @Column(name = "id", length = 32, nullable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "zone_id", nullable = false, foreignKey = @ForeignKey(name = "fk_inspection_zone"))
    private Zone zone;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alert_id", foreignKey = @ForeignKey(name = "fk_inspection_alert"))
    private Alert alert;

    @Column(name = "inspector_name", length = 64, nullable = false)
    private String inspectorName;

    @Column(name = "inspection_date", nullable = false)
    private LocalDateTime inspectionDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 32, nullable = false)
    private InspectionStatus status = InspectionStatus.PENDING;

    @Column(name = "observations", length = 1024)
    private String observations;

    @Column(name = "findings_summary", length = 512)
    private String findingsSummary;

    @Column(name = "action_required", nullable = false)
    private Boolean actionRequired = false;

    public Inspection() {}

    public Inspection(String id, Zone zone, Alert alert, String inspectorName, LocalDateTime inspectionDate,
                      InspectionStatus status, String observations, String findingsSummary, Boolean actionRequired) {
        this.id = id;
        this.zone = zone;
        this.alert = alert;
        this.inspectorName = inspectorName;
        this.inspectionDate = inspectionDate != null ? inspectionDate : LocalDateTime.now();
        this.status = status != null ? status : InspectionStatus.PENDING;
        this.observations = observations;
        this.findingsSummary = findingsSummary;
        this.actionRequired = actionRequired != null ? actionRequired : false;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public Zone getZone() { return zone; }
    public void setZone(Zone zone) { this.zone = zone; }

    public String getZoneId() { return zone != null ? zone.getId() : null; }

    public Alert getAlert() { return alert; }
    public void setAlert(Alert alert) { this.alert = alert; }

    public String getAlertId() { return alert != null ? alert.getId() : null; }

    public String getInspectorName() { return inspectorName; }
    public void setInspectorName(String inspectorName) { this.inspectorName = inspectorName; }

    public LocalDateTime getInspectionDate() { return inspectionDate; }
    public void setInspectionDate(LocalDateTime inspectionDate) { this.inspectionDate = inspectionDate; }

    public InspectionStatus getStatus() { return status; }
    public void setStatus(InspectionStatus status) { this.status = status; }

    public String getObservations() { return observations; }
    public void setObservations(String observations) { this.observations = observations; }

    public String getFindingsSummary() { return findingsSummary; }
    public void setFindingsSummary(String findingsSummary) { this.findingsSummary = findingsSummary; }

    public Boolean getActionRequired() { return actionRequired; }
    public void setActionRequired(Boolean actionRequired) { this.actionRequired = actionRequired; }
}

