package com.warehouse.riskmonitor.repository;

import com.warehouse.riskmonitor.entity.AlarmLotImpact;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AlarmLotImpactRepository extends JpaRepository<AlarmLotImpact, String> {
    List<AlarmLotImpact> findByAlert_Id(String alertId);
    List<AlarmLotImpact> findByLot_Id(String lotId);
    Page<AlarmLotImpact> findAll(Pageable pageable);
}
