package com.warehouse.riskmonitor.repository;

import com.warehouse.riskmonitor.entity.LotZoneAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface LotZoneAssignmentRepository extends JpaRepository<LotZoneAssignment, String> {
    List<LotZoneAssignment> findByLot_Id(String lotId);
    List<LotZoneAssignment> findByZone_Id(String zoneId);

    @Query("SELECT lza FROM LotZoneAssignment lza WHERE lza.zone.id = :zoneId " +
           "AND lza.ingressTimestamp <= :end " +
           "AND (lza.egressTimestamp IS NULL OR lza.egressTimestamp >= :start)")
    List<LotZoneAssignment> findLotsInZoneDuringInterval(@Param("zoneId") String zoneId,
                                                        @Param("start") LocalDateTime start,
                                                        @Param("end") LocalDateTime end);
}
