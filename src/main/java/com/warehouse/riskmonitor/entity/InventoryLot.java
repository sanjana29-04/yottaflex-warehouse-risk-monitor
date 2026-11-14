package com.warehouse.riskmonitor.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.warehouse.riskmonitor.entity.enums.LotStatus;
import com.warehouse.riskmonitor.entity.enums.ProductCategory;
import com.warehouse.riskmonitor.entity.enums.UnitOfMeasure;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "inventory_lots", indexes = {
    @Index(name = "idx_lot_zone", columnList = "current_zone_id"),
    @Index(name = "idx_lot_req", columnList = "storage_requirement_id"),
    @Index(name = "idx_lot_status", columnList = "status"),
    @Index(name = "idx_lot_sku", columnList = "sku")
})
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class InventoryLot {

    @Id
    @Column(name = "id", length = 32, nullable = false)
    private String id;

    @Column(name = "product_name", length = 128, nullable = false)
    private String productName;

    @Column(name = "sku", length = 64, nullable = false)
    private String sku;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", length = 64, nullable = false)
    private ProductCategory category;

    @Column(name = "quantity", nullable = false)
    private Double quantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "unit", length = 32, nullable = false)
    private UnitOfMeasure unit = UnitOfMeasure.PALLETS;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "storage_requirement_id", nullable = false, foreignKey = @ForeignKey(name = "fk_lot_requirement"))
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private StorageRequirement storageRequirement;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_zone_id", nullable = false, foreignKey = @ForeignKey(name = "fk_lot_zone"))
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "warehouse", "sensors"})
    private Zone currentZone;

    @Column(name = "ingress_timestamp", nullable = false)
    private LocalDateTime ingressTimestamp;

    @Column(name = "expiry_date", nullable = false)
    private LocalDateTime expiryDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 32, nullable = false)
    private LotStatus status = LotStatus.STORED;

    @Column(name = "exposure_duration_minutes", nullable = false)
    private Double exposureDurationMinutes = 0.0;

    @Column(name = "thermal_stress_score", nullable = false)
    private Double thermalStressScore = 0.0;

    public InventoryLot() {}

    public InventoryLot(String id, String productName, String sku, ProductCategory category,
                        Double quantity, UnitOfMeasure unit, StorageRequirement storageRequirement,
                        Zone currentZone, LocalDateTime ingressTimestamp, LocalDateTime expiryDate,
                        LotStatus status) {
        this.id = id;
        this.productName = productName;
        this.sku = sku;
        this.category = category;
        this.quantity = quantity;
        this.unit = unit != null ? unit : UnitOfMeasure.PALLETS;
        this.storageRequirement = storageRequirement;
        this.currentZone = currentZone;
        this.ingressTimestamp = ingressTimestamp;
        this.expiryDate = expiryDate;
        this.status = status != null ? status : LotStatus.STORED;
        this.exposureDurationMinutes = 0.0;
        this.thermalStressScore = 0.0;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }

    public ProductCategory getCategory() { return category; }
    public void setCategory(ProductCategory category) { this.category = category; }

    public Double getQuantity() { return quantity; }
    public void setQuantity(Double quantity) { this.quantity = quantity; }

    public UnitOfMeasure getUnit() { return unit; }
    public void setUnit(UnitOfMeasure unit) { this.unit = unit; }

    public StorageRequirement getStorageRequirement() { return storageRequirement; }
    public void setStorageRequirement(StorageRequirement storageRequirement) { this.storageRequirement = storageRequirement; }

    public String getStorageRequirementId() {
        return storageRequirement != null ? storageRequirement.getId() : null;
    }

    public Zone getCurrentZone() { return currentZone; }
    public void setCurrentZone(Zone currentZone) { this.currentZone = currentZone; }

    public String getCurrentZoneId() {
        return currentZone != null ? currentZone.getId() : null;
    }

    public LocalDateTime getIngressTimestamp() { return ingressTimestamp; }
    public void setIngressTimestamp(LocalDateTime ingressTimestamp) { this.ingressTimestamp = ingressTimestamp; }

    public LocalDateTime getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDateTime expiryDate) { this.expiryDate = expiryDate; }

    public LotStatus getStatus() { return status; }
    public void setStatus(LotStatus status) { this.status = status; }

    public Double getExposureDurationMinutes() { return exposureDurationMinutes; }
    public void setExposureDurationMinutes(Double exposureDurationMinutes) { this.exposureDurationMinutes = exposureDurationMinutes; }

    public Double getThermalStressScore() { return thermalStressScore; }
    public void setThermalStressScore(Double thermalStressScore) { this.thermalStressScore = thermalStressScore; }
}
