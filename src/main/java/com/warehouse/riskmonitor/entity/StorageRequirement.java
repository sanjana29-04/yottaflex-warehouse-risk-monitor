package com.warehouse.riskmonitor.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.warehouse.riskmonitor.entity.enums.ProductCategory;
import jakarta.persistence.*;

@Entity
@Table(name = "storage_requirements", indexes = {
    @Index(name = "idx_storage_cat", columnList = "category")
})
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class StorageRequirement {

    @Id
    @Column(name = "id", length = 32, nullable = false)
    private String id;

    @Column(name = "name", length = 128, nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", length = 64, nullable = false)
    private ProductCategory category;

    @Column(name = "temp_target", nullable = false)
    private Double tempTarget;

    @Column(name = "temp_min", nullable = false)
    private Double tempMin;

    @Column(name = "temp_max", nullable = false)
    private Double tempMax;

    @Column(name = "humidity_target", nullable = false)
    private Double humidityTarget;

    @Column(name = "humidity_min", nullable = false)
    private Double humidityMin;

    @Column(name = "humidity_max", nullable = false)
    private Double humidityMax;

    @Column(name = "max_door_open_seconds", nullable = false)
    private Integer maxDoorOpenSeconds;

    public StorageRequirement() {}

    public StorageRequirement(String id, String name, ProductCategory category,
                              Double tempTarget, Double tempMin, Double tempMax,
                              Double humidityTarget, Double humidityMin, Double humidityMax,
                              Integer maxDoorOpenSeconds) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.tempTarget = tempTarget;
        this.tempMin = tempMin;
        this.tempMax = tempMax;
        this.humidityTarget = humidityTarget;
        this.humidityMin = humidityMin;
        this.humidityMax = humidityMax;
        this.maxDoorOpenSeconds = maxDoorOpenSeconds;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public ProductCategory getCategory() { return category; }
    public void setCategory(ProductCategory category) { this.category = category; }

    public Double getTempTarget() { return tempTarget; }
    public void setTempTarget(Double tempTarget) { this.tempTarget = tempTarget; }

    public Double getTempMin() { return tempMin; }
    public void setTempMin(Double tempMin) { this.tempMin = tempMin; }

    public Double getTempMax() { return tempMax; }
    public void setTempMax(Double tempMax) { this.tempMax = tempMax; }

    public Double getHumidityTarget() { return humidityTarget; }
    public void setHumidityTarget(Double humidityTarget) { this.humidityTarget = humidityTarget; }

    public Double getHumidityMin() { return humidityMin; }
    public void setHumidityMin(Double humidityMin) { this.humidityMin = humidityMin; }

    public Double getHumidityMax() { return humidityMax; }
    public void setHumidityMax(Double humidityMax) { this.humidityMax = humidityMax; }

    public Integer getMaxDoorOpenSeconds() { return maxDoorOpenSeconds; }
    public void setMaxDoorOpenSeconds(Integer maxDoorOpenSeconds) { this.maxDoorOpenSeconds = maxDoorOpenSeconds; }
}
