package com.warehouse.riskmonitor.repository;

import com.warehouse.riskmonitor.entity.Zone;
import com.warehouse.riskmonitor.entity.enums.ZoneStatus;
import com.warehouse.riskmonitor.entity.enums.ZoneType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ZoneRepository extends JpaRepository<Zone, String> {
    List<Zone> findByWarehouse_Id(String warehouseId);
    List<Zone> findByStatus(ZoneStatus status);
    List<Zone> findByZoneType(ZoneType zoneType);

    long countByStatus(ZoneStatus status);

    @Query(value = "SELECT z FROM Zone z JOIN FETCH z.warehouse WHERE (:status IS NULL OR z.status = :status) " +
                   "AND (:whId IS NULL OR z.warehouse.id = :whId) " +
                   "AND (:search IS NULL OR LOWER(z.name) LIKE LOWER(CONCAT('%', :search, '%')))",
           countQuery = "SELECT COUNT(z) FROM Zone z WHERE (:status IS NULL OR z.status = :status) " +
                        "AND (:whId IS NULL OR z.warehouse.id = :whId) " +
                        "AND (:search IS NULL OR LOWER(z.name) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Zone> findFiltered(@Param("status") ZoneStatus status,
                            @Param("whId") String whId,
                            @Param("search") String search,
                            Pageable pageable);
}
