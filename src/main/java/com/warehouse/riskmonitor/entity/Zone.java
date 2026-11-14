package com.warehouse.riskmonitor.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.warehouse.riskmonitor.entity.enums.ZoneStatus;
import com.warehouse.riskmonitor.entity.enums.ZoneType;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "zones", indexes = {
    @Index(name = "idx_zone_wh", columnList = "warehouse_id"),
    @Index(name = "idx_zone_type", columnList = "zone_type"),
    @Index(name = "idx_zone_status", columnList = "status")
})
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Zone {

    @Id
    @Column(name = "id", length = 48, nullable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id", nullable = false, foreignKey = @ForeignKey(name = "fk_zone_warehouse"))
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "zones"})
    private Warehouse warehouse;

    @Column(name = "name", length = 128, nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "zone_type", length = 32, nullable = false)
    private ZoneType zoneType;

    @Column(name = "capacity_lots", nullable = false)
    private Integer capacityLots;

    @Column(name = "current_temperature")
    private Double currentTemperature;

    @Column(name = "current_humidity")
    private Double currentHumidity;

    @Column(name = "current_occupancy")
    private Integer currentOccupancy = 0;

    @Column(name = "max_occupancy", nullable = false)
    private Integer maxOccupancy = 10;

    @Column(name = "temperature_min", nullable = false)
    private Double temperatureMin;

    @Column(name = "temperature_max", nullable = false)
    private Double temperatureMax;

    @Column(name = "humidity_min", nullable = false)
    private Double humidityMin;

    @Column(name = "humidity_max", nullable = false)
    private Double humidityMax;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 32, nullable = false)
    private ZoneStatus status = ZoneStatus.NORMAL;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @JsonIgnore
    @OneToMany(mappedBy = "zone", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Sensor> sensors = new ArrayList<>();

    public Zone() {}

    public Zone(String id, Warehouse warehouse, String name, ZoneType zoneType, Integer capacityLots,
                Double temperatureMin, Double temperatureMax, Double humidityMin, Double humidityMax,
                Integer maxOccupancy) {
        this.id = id;
        this.warehouse = warehouse;
        this.name = name;
        this.zoneType = zoneType;
        this.capacityLots = capacityLots;
        this.temperatureMin = temperatureMin;
        this.temperatureMax = temperatureMax;
        this.humidityMin = humidityMin;
        this.humidityMax = humidityMax;
        this.maxOccupancy = maxOccupancy;
        this.status = ZoneStatus.NORMAL;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (updatedAt == null) updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public Warehouse getWarehouse() { return warehouse; }
    public void setWarehouse(Warehouse warehouse) { this.warehouse = warehouse; }

    public String getWarehouseId() {
        return warehouse != null ? warehouse.getId() : null;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public ZoneType getZoneType() { return zoneType; }
    public void setZoneType(ZoneType zoneType) { this.zoneType = zoneType; }

    public Integer getCapacityLots() { return capacityLots; }
    public void setCapacityLots(Integer capacityLots) { this.capacityLots = capacityLots; }

    public Double getCurrentTemperature() { return currentTemperature; }
    public void setCurrentTemperature(Double currentTemperature) { this.currentTemperature = currentTemperature; }

    public Double getCurrentHumidity() { return currentHumidity; }
    public void setCurrentHumidity(Double currentHumidity) { this.currentHumidity = currentHumidity; }

    public Integer getCurrentOccupancy() { return currentOccupancy; }
    public void setCurrentOccupancy(Integer currentOccupancy) { this.currentOccupancy = currentOccupancy; }

    public Integer getMaxOccupancy() { return maxOccupancy; }
    public void setMaxOccupancy(Integer maxOccupancy) { this.maxOccupancy = maxOccupancy; }

    public Double getTemperatureMin() { return temperatureMin; }
    public void setTemperatureMin(Double temperatureMin) { this.temperatureMin = temperatureMin; }

    public Double getTemperatureMax() { return temperatureMax; }
    public void setTemperatureMax(Double temperatureMax) { this.temperatureMax = temperatureMax; }

    public Double getHumidityMin() { return humidityMin; }
    public void setHumidityMin(Double humidityMin) { this.humidityMin = humidityMin; }

    public Double getHumidityMax() { return humidityMax; }
    public void setHumidityMax(Double humidityMax) { this.humidityMax = humidityMax; }

    public ZoneStatus getStatus() { return status; }
    public void setStatus(ZoneStatus status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public List<Sensor> getSensors() { return sensors; }
    public void setSensors(List<Sensor> sensors) { this.sensors = sensors; }
}
