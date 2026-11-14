package com.warehouse.riskmonitor.repository;

import com.warehouse.riskmonitor.entity.Remediation;
import com.warehouse.riskmonitor.entity.enums.RemediationStatus;
import com.warehouse.riskmonitor.entity.enums.RemediationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface RemediationRepository extends JpaRepository<Remediation, String> {
    List<Remediation> findByAlert_Id(String alertId);
    List<Remediation> findByStatus(RemediationStatus status);

    @Query("SELECT r FROM Remediation r WHERE (:status IS NULL OR r.status = :status) " +
           "AND (:type IS NULL OR r.actionType = :type) " +
           "ORDER BY r.scheduledAt DESC")
    Page<Remediation> findFiltered(@Param("status") RemediationStatus status,
                                   @Param("type") RemediationType type,
                                   Pageable pageable);

    @Query("SELECT SUM(r.costImpact) FROM Remediation r")
    Double calculateTotalCostImpact();

    long countByStatus(RemediationStatus status);
}
