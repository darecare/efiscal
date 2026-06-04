package com.efiscal.backend.repository;

import com.efiscal.backend.model.ProductEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<ProductEntity, Long> {

    Page<ProductEntity> findAllByOrgIdAndDeletedAtIsNullOrderByNameAsc(Long orgId, Pageable pageable);

    long countByOrgIdAndDeletedAtIsNull(Long orgId);

    Optional<ProductEntity> findByProductIdAndDeletedAtIsNull(Long productId);

    Optional<ProductEntity> findByOrgIdAndMpProductIdAndDeletedAtIsNull(Long orgId, Long mpProductId);

    Optional<ProductEntity> findByOrgIdAndSkuIgnoreCaseAndDeletedAtIsNull(Long orgId, String sku);

    Optional<ProductEntity> findByOrgIdAndEanAndDeletedAtIsNull(Long orgId, String ean);

    @Query("""
        SELECT p FROM ProductEntity p
        WHERE p.orgId = :orgId AND p.deletedAt IS NULL AND p.isActive = true
        AND (:name IS NULL OR :name = '' OR LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%')))
        AND (:sku IS NULL OR :sku = '' OR LOWER(p.sku) = LOWER(:sku))
        AND (:ean IS NULL OR :ean = '' OR LOWER(p.ean) = LOWER(:ean))
        ORDER BY p.name ASC
        """)
    List<ProductEntity> search(
        @Param("orgId") Long orgId,
        @Param("name") String name,
        @Param("sku") String sku,
        @Param("ean") String ean,
        Pageable pageable
    );

    @Query("""
        SELECT p FROM ProductEntity p
        WHERE p.orgId = :orgId AND p.deletedAt IS NULL AND p.isActive = true
        AND (
            LOWER(p.name) LIKE LOWER(CONCAT('%', :term, '%'))
            OR LOWER(p.sku) = LOWER(:term)
            OR LOWER(p.ean) = LOWER(:term)
        )
        ORDER BY p.name ASC
        """)
    List<ProductEntity> searchByTerm(
        @Param("orgId") Long orgId,
        @Param("term") String term,
        Pageable pageable
    );
}
