package com.warehouse.riskmonitor.repository;

import com.warehouse.riskmonitor.entity.InventoryLot;
import com.warehouse.riskmonitor.entity.enums.LotStatus;
import com.warehouse.riskmonitor.entity.enums.ProductCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface InventoryLotRepository extends JpaRepository<InventoryLot, String> {
    List<InventoryLot> findByCurrentZone_Id(String zoneId);
    List<InventoryLot> findByStatus(LotStatus status);
    List<InventoryLot> findByCategory(ProductCategory category);

    @Query("SELECT l FROM InventoryLot l WHERE (:zoneId IS NULL OR l.currentZone.id = :zoneId) " +
           "AND (:status IS NULL OR l.status = :status) " +
           "AND (:category IS NULL OR l.category = :category) " +
           "AND (:search IS NULL OR LOWER(l.productName) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "     OR LOWER(l.sku) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "     OR LOWER(l.id) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<InventoryLot> findFiltered(@Param("zoneId") String zoneId,
                                    @Param("status") LotStatus status,
                                    @Param("category") ProductCategory category,
                                    @Param("search") String search,
                                    Pageable pageable);

    long countByStatus(LotStatus status);

    @Query("SELECT COUNT(l) FROM InventoryLot l WHERE l.thermalStressScore > 0 OR l.status = 'QUARANTINED'")
    long countCompromisedLots();
}
