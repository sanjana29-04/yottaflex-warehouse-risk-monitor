package com.warehouse.riskmonitor.repository;

import com.warehouse.riskmonitor.entity.SensorReading;
import com.warehouse.riskmonitor.entity.enums.ReadingStatus;
import com.warehouse.riskmonitor.entity.enums.SensorType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SensorReadingRepository extends JpaRepository<SensorReading, String> {
    List<SensorReading> findBySensor_IdOrderByTimestampDesc(String sensorId);
    Page<SensorReading> findBySensor_Id(String sensorId, Pageable pageable);
    Page<SensorReading> findByZone_Id(String zoneId, Pageable pageable);
    Page<SensorReading> findBySensor_IdOrderByTimestampDesc(String sensorId, Pageable pageable);
    Page<SensorReading> findByZone_IdOrderByTimestampDesc(String zoneId, Pageable pageable);

    @Query("SELECT r FROM SensorReading r ORDER BY r.timestamp DESC")
    List<SensorReading> findRecentReadings(Pageable pageable);

    @Query("SELECT r FROM SensorReading r WHERE r.readingType = :type ORDER BY r.timestamp DESC")
    List<SensorReading> findTopReadingsByType(@Param("type") SensorType type, Pageable pageable);

    @Query("SELECT r FROM SensorReading r WHERE (:zoneId IS NULL OR r.zone.id = :zoneId) " +
           "AND (:sensorId IS NULL OR r.sensor.id = :sensorId) " +
           "AND (:type IS NULL OR r.readingType = :type) " +
           "AND (:status IS NULL OR r.status = :status) " +
           "ORDER BY r.timestamp DESC")
    Page<SensorReading> findFiltered(@Param("zoneId") String zoneId,
                                     @Param("sensorId") String sensorId,
                                     @Param("type") SensorType type,
                                     @Param("status") ReadingStatus status,
                                     Pageable pageable);

    @Query("SELECT r FROM SensorReading r WHERE r.sensor.id = :sensorId " +
           "AND r.timestamp BETWEEN :start AND :end ORDER BY r.timestamp ASC")
    List<SensorReading> findBySensorAndTimeRange(@Param("sensorId") String sensorId,
                                                @Param("start") LocalDateTime start,
                                                @Param("end") LocalDateTime end);

    @Query("SELECT r FROM SensorReading r WHERE r.zone.id = :zoneId AND r.readingType = :type " +
           "ORDER BY r.timestamp DESC")
    List<SensorReading> findTop50ByZoneAndType(@Param("zoneId") String zoneId,
                                              @Param("type") SensorType type,
                                              Pageable pageable);

    long countByStatus(ReadingStatus status);
}
