package com.warehouse.riskmonitor.repository;

import com.warehouse.riskmonitor.entity.Warehouse;
import com.warehouse.riskmonitor.entity.enums.WarehouseStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface WarehouseRepository extends JpaRepository<Warehouse, String> {
    List<Warehouse> findByStatus(WarehouseStatus status);
    Page<Warehouse> findByNameContainingIgnoreCaseOrLocationContainingIgnoreCase(String name, String location, Pageable pageable);

    @Query("SELECT COUNT(w) FROM Warehouse w")
    long countTotalWarehouses();
}

