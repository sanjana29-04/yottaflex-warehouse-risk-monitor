package com.warehouse.riskmonitor.repository;

import com.warehouse.riskmonitor.entity.SensorZoneMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SensorZoneMappingRepository extends JpaRepository<SensorZoneMapping, String> {

    List<SensorZoneMapping> findBySensor_Id(String sensorId);

    List<SensorZoneMapping> findByZone_Id(String zoneId);

    @Query("SELECT m FROM SensorZoneMapping m WHERE m.isActive = true")
    List<SensorZoneMapping> findActiveMappings();

    @Query("SELECT m.sensor.id, COUNT(m) FROM SensorZoneMapping m WHERE m.isActive = true GROUP BY m.sensor.id HAVING COUNT(m) > 1")
    List<Object[]> findSensorsMappedToMultipleActiveZones();
}

