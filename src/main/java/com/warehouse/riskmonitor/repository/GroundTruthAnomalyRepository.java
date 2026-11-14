package com.warehouse.riskmonitor.repository;

import com.warehouse.riskmonitor.entity.GroundTruthAnomaly;
import com.warehouse.riskmonitor.entity.enums.AnomalyType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface GroundTruthAnomalyRepository extends JpaRepository<GroundTruthAnomaly, String> {
    List<GroundTruthAnomaly> findByAnomalyType(AnomalyType anomalyType);
    List<GroundTruthAnomaly> findByDetected(Boolean detected);
}

