package com.efiscal.backend.repository;

import com.efiscal.backend.model.TaxCategoryEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaxCategoryRepository extends JpaRepository<TaxCategoryEntity, Long> {
    List<TaxCategoryEntity> findAllByDeletedAtIsNull();
}
