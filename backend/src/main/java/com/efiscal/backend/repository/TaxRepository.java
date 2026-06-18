package com.efiscal.backend.repository;

import com.efiscal.backend.model.TaxEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface TaxRepository extends JpaRepository<TaxEntity, Long> {
    List<TaxEntity> findAllByDeletedAtIsNull();
    List<TaxEntity> findAllByDeletedAtIsNullAndIsActiveTrue();

    @Query("SELECT t FROM TaxEntity t JOIN t.taxCategory c " +
           "WHERE t.deletedAt IS NULL AND t.isActive = true " +
           "AND c.deletedAt IS NULL AND c.isActive = true " +
           "AND UPPER(c.name) = UPPER(:taxCategoryName)")
    List<TaxEntity> findActiveTaxesByCategoryName(String taxCategoryName);
}
