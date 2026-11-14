package com.warehouse.riskmonitor.repository;

import com.warehouse.riskmonitor.entity.DoorEvent;
import com.warehouse.riskmonitor.entity.enums.DoorStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DoorEventRepository extends JpaRepository<DoorEvent, String> {
    List<DoorEvent> findByZone_Id(String zoneId);
    List<DoorEvent> findByStatus(DoorStatus status);

    @Query("SELECT d FROM DoorEvent d WHERE (:zoneId IS NULL OR d.zone.id = :zoneId) " +
           "AND (:status IS NULL OR d.status = :status) " +
           "ORDER BY d.openedAt DESC")
    Page<DoorEvent> findFiltered(@Param("zoneId") String zoneId,
                                 @Param("status") DoorStatus status,
                                 Pageable pageable);

    long countByStatus(DoorStatus status);
}
