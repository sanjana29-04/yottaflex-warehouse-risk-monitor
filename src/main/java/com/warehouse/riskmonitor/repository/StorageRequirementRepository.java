package com.warehouse.riskmonitor.repository;

import com.warehouse.riskmonitor.entity.StorageRequirement;
import com.warehouse.riskmonitor.entity.enums.ProductCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface StorageRequirementRepository extends JpaRepository<StorageRequirement, String> {
    List<StorageRequirement> findByCategory(ProductCategory category);
}

