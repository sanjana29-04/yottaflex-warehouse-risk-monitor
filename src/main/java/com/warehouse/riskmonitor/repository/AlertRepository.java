package com.warehouse.riskmonitor.repository;

import com.warehouse.riskmonitor.entity.Alert;
import com.warehouse.riskmonitor.entity.enums.AlertStatus;
import com.warehouse.riskmonitor.entity.enums.AlertType;
import com.warehouse.riskmonitor.entity.enums.RiskLevel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AlertRepository extends JpaRepository<Alert, String> {
    List<Alert> findByStatus(AlertStatus status);

    List<Alert> findByRiskLevel(RiskLevel riskLevel);

    List<Alert> findByZone_Id(String zoneId);

    @Query("SELECT a FROM Alert a WHERE (:zoneId IS NULL OR a.zone.id = :zoneId) " +
            "AND (:status IS NULL OR a.status = :status) " +
            "AND (:risk IS NULL OR a.riskLevel = :risk) " +
            "AND (:type IS NULL OR a.alertType = :type) " +
            "ORDER BY a.triggeredAt DESC")
    Page<Alert> findFiltered(@Param("zoneId") String zoneId,
            @Param("status") AlertStatus status,
            @Param("risk") RiskLevel risk,
            @Param("type") AlertType type,
            Pageable pageable);

    @Query("SELECT a FROM Alert a WHERE a.status = 'OPEN' ORDER BY a.triggeredAt DESC")
    List<Alert> findTopOpenAlerts(Pageable pageable);

    long countByStatus(AlertStatus status);

    long countByRiskLevel(RiskLevel riskLevel);

    long countByStatusAndRiskLevel(AlertStatus status, RiskLevel riskLevel);

    @Query("SELECT a.riskLevel, COUNT(a) FROM Alert a GROUP BY a.riskLevel")
    List<Object[]> countByRiskLevelGroup();
}
