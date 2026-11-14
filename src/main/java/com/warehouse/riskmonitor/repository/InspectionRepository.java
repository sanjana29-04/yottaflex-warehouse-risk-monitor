package com.warehouse.riskmonitor.repository;

import com.warehouse.riskmonitor.entity.Inspection;
import com.warehouse.riskmonitor.entity.enums.InspectionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface InspectionRepository extends JpaRepository<Inspection, String> {
    List<Inspection> findByZone_Id(String zoneId);
    List<Inspection> findByStatus(InspectionStatus status);

    @Query("SELECT i FROM Inspection i WHERE (:zoneId IS NULL OR i.zone.id = :zoneId) " +
           "AND (:status IS NULL OR i.status = :status) " +
           "ORDER BY i.inspectionDate DESC")
    Page<Inspection> findFiltered(@Param("zoneId") String zoneId,
                                  @Param("status") InspectionStatus status,
                                  Pageable pageable);

    long countByStatus(InspectionStatus status);
}
