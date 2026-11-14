package com.warehouse.riskmonitor.repository;

import com.warehouse.riskmonitor.entity.Sensor;
import com.warehouse.riskmonitor.entity.enums.SensorStatus;
import com.warehouse.riskmonitor.entity.enums.SensorType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SensorRepository extends JpaRepository<Sensor, String> {
    List<Sensor> findByZone_Id(String zoneId);
    List<Sensor> findBySensorType(SensorType sensorType);
    List<Sensor> findByStatus(SensorStatus status);

    @Query("SELECT s FROM Sensor s WHERE (:type IS NULL OR s.sensorType = :type) " +
           "AND (:status IS NULL OR s.status = :status) " +
           "AND (:zoneId IS NULL OR s.zone.id = :zoneId)")
    Page<Sensor> findFiltered(@Param("type") SensorType type,
                              @Param("status") SensorStatus status,
                              @Param("zoneId") String zoneId,
                              Pageable pageable);

    @Query("SELECT s.sensorType, COUNT(s) FROM Sensor s GROUP BY s.sensorType")
    List<Object[]> countBySensorType();
}
