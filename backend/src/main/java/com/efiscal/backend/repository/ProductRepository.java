package com.efiscal.backend.repository;

import com.efiscal.backend.model.ProductEntity;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<ProductEntity, Long> {

    @Query("""
        SELECT p FROM ProductEntity p
        WHERE p.orgId = :orgId AND p.deletedAt IS NULL AND p.hiddenAt IS NULL
        ORDER BY p.name ASC
        """)
    Page<ProductEntity> findAllVisibleByOrgIdOrderByNameAsc(
        @Param("orgId") Long orgId,
        Pageable pageable
    );

    @Query("""
        SELECT p FROM ProductEntity p
        WHERE p.orgId = :orgId AND p.deletedAt IS NULL AND p.hiddenAt IS NULL
        AND (
            LOWER(p.name) LIKE :term
            OR LOWER(p.sku) LIKE :term
            OR LOWER(p.ean) LIKE :term
            OR CAST(p.productId AS string) LIKE :term
            OR (p.mpProductId IS NOT NULL AND CAST(p.mpProductId AS string) LIKE :term)
            OR (p.lastKnownPrice IS NOT NULL AND CAST(p.lastKnownPrice AS string) LIKE :term)
        )
        ORDER BY p.name ASC
        """)
    Page<ProductEntity> findAllVisibleByOrgIdAndSearchTerm(
        @Param("orgId") Long orgId,
        @Param("term") String term,
        Pageable pageable
    );

    @Query("""
        SELECT p.productId FROM ProductEntity p
        WHERE p.orgId = :orgId AND p.deletedAt IS NULL AND p.hiddenAt IS NULL
        ORDER BY p.name ASC
        """)
    List<Long> findVisibleIdsByOrgId(@Param("orgId") Long orgId, Pageable pageable);

    @Query("""
        SELECT p.productId FROM ProductEntity p
        WHERE p.orgId = :orgId AND p.deletedAt IS NULL AND p.hiddenAt IS NULL
        AND (
            LOWER(p.name) LIKE :term
            OR LOWER(p.sku) LIKE :term
            OR LOWER(p.ean) LIKE :term
            OR CAST(p.productId AS string) LIKE :term
            OR (p.mpProductId IS NOT NULL AND CAST(p.mpProductId AS string) LIKE :term)
            OR (p.lastKnownPrice IS NOT NULL AND CAST(p.lastKnownPrice AS string) LIKE :term)
        )
        ORDER BY p.name ASC
        """)
    List<Long> findVisibleIdsByOrgIdAndSearchTerm(
        @Param("orgId") Long orgId,
        @Param("term") String term,
        Pageable pageable
    );

    @Modifying(clearAutomatically = true)
    @Query("""
        UPDATE ProductEntity p SET p.hiddenAt = :now
        WHERE p.orgId = :orgId AND p.deletedAt IS NULL AND p.hiddenAt IS NULL
        AND p.sourceType = 'MERCHANTPRO'
        AND (
            :term IS NULL
            OR LOWER(p.name) LIKE :term
            OR LOWER(p.sku) LIKE :term
            OR LOWER(p.ean) LIKE :term
            OR CAST(p.productId AS string) LIKE :term
            OR (p.mpProductId IS NOT NULL AND CAST(p.mpProductId AS string) LIKE :term)
            OR (p.lastKnownPrice IS NOT NULL AND CAST(p.lastKnownPrice AS string) LIKE :term)
        )
        """)
    int hideAllSyncedByOrgIdAndOptionalSearchTerm(
        @Param("orgId") Long orgId,
        @Param("term") String term,
        @Param("now") OffsetDateTime now
    );

    @Modifying(clearAutomatically = true)
    @Query("""
        UPDATE ProductEntity p SET p.deletedAt = :now
        WHERE p.orgId = :orgId AND p.deletedAt IS NULL AND p.hiddenAt IS NULL
        AND p.sourceType = 'MANUAL'
        AND (
            :term IS NULL
            OR LOWER(p.name) LIKE :term
            OR LOWER(p.sku) LIKE :term
            OR LOWER(p.ean) LIKE :term
            OR CAST(p.productId AS string) LIKE :term
            OR (p.mpProductId IS NOT NULL AND CAST(p.mpProductId AS string) LIKE :term)
            OR (p.lastKnownPrice IS NOT NULL AND CAST(p.lastKnownPrice AS string) LIKE :term)
        )
        """)
    int softDeleteAllManualByOrgIdAndOptionalSearchTerm(
        @Param("orgId") Long orgId,
        @Param("term") String term,
        @Param("now") OffsetDateTime now
    );

    @Modifying(clearAutomatically = true)
    @Query("""
        UPDATE ProductEntity p SET p.isActive = :isActive
        WHERE p.orgId = :orgId AND p.deletedAt IS NULL AND p.hiddenAt IS NULL
        AND (
            :term IS NULL
            OR LOWER(p.name) LIKE :term
            OR LOWER(p.sku) LIKE :term
            OR LOWER(p.ean) LIKE :term
            OR CAST(p.productId AS string) LIKE :term
            OR (p.mpProductId IS NOT NULL AND CAST(p.mpProductId AS string) LIKE :term)
            OR (p.lastKnownPrice IS NOT NULL AND CAST(p.lastKnownPrice AS string) LIKE :term)
        )
        """)
    int updateStatusAllVisibleByOrgIdAndOptionalSearchTerm(
        @Param("orgId") Long orgId,
        @Param("term") String term,
        @Param("isActive") boolean isActive
    );

    @Modifying(clearAutomatically = true)
    @Query("""
        UPDATE ProductEntity p SET p.hiddenAt = :now
        WHERE p.productId IN :ids AND p.orgId = :orgId AND p.deletedAt IS NULL AND p.hiddenAt IS NULL
        AND p.sourceType = 'MERCHANTPRO'
        """)
    int hideSyncedByIdsAndOrgId(
        @Param("ids") List<Long> ids,
        @Param("orgId") Long orgId,
        @Param("now") OffsetDateTime now
    );

    @Modifying(clearAutomatically = true)
    @Query("""
        UPDATE ProductEntity p SET p.deletedAt = :now
        WHERE p.productId IN :ids AND p.orgId = :orgId AND p.deletedAt IS NULL AND p.hiddenAt IS NULL
        AND p.sourceType = 'MANUAL'
        """)
    int softDeleteManualByIdsAndOrgId(
        @Param("ids") List<Long> ids,
        @Param("orgId") Long orgId,
        @Param("now") OffsetDateTime now
    );

    @Modifying(clearAutomatically = true)
    @Query("""
        UPDATE ProductEntity p SET p.isActive = :isActive
        WHERE p.productId IN :ids AND p.orgId = :orgId AND p.deletedAt IS NULL AND p.hiddenAt IS NULL
        """)
    int updateStatusVisibleByIdsAndOrgId(
        @Param("ids") List<Long> ids,
        @Param("orgId") Long orgId,
        @Param("isActive") boolean isActive
    );

    @Query("""
        SELECT COUNT(p) FROM ProductEntity p
        WHERE p.orgId = :orgId AND p.deletedAt IS NULL AND p.hiddenAt IS NULL
        """)
    long countVisibleByOrgId(@Param("orgId") Long orgId);

    @Query("""
        SELECT COUNT(p) FROM ProductEntity p
        WHERE p.orgId = :orgId AND p.sourceType = 'MERCHANTPRO'
        AND p.deletedAt IS NULL AND p.hiddenAt IS NOT NULL
        """)
    long countHiddenMerchantProByOrgId(@Param("orgId") Long orgId);

    @Query("""
        SELECT p FROM ProductEntity p
        WHERE p.productId = :productId AND p.deletedAt IS NULL AND p.hiddenAt IS NULL
        """)
    Optional<ProductEntity> findVisibleByProductId(@Param("productId") Long productId);

    Optional<ProductEntity> findByOrgIdAndMpProductIdAndDeletedAtIsNull(Long orgId, Long mpProductId);

    @Query("""
        SELECT p FROM ProductEntity p
        WHERE p.orgId = :orgId AND p.mpProductId = :mpProductId AND p.deletedAt IS NULL
        """)
    Optional<ProductEntity> findByOrgIdAndMpProductIdIncludingHidden(
        @Param("orgId") Long orgId,
        @Param("mpProductId") Long mpProductId
    );

    @Query("""
        SELECT p FROM ProductEntity p
        WHERE p.orgId = :orgId AND p.sourceType = 'MERCHANTPRO' AND p.deletedAt IS NULL
        AND LOWER(p.sku) = LOWER(:sku)
        """)
    Optional<ProductEntity> findMerchantProByOrgIdAndSkuIncludingHidden(
        @Param("orgId") Long orgId,
        @Param("sku") String sku
    );

    @Query("""
        SELECT p FROM ProductEntity p
        WHERE p.orgId = :orgId AND p.sourceType = 'MERCHANTPRO' AND p.deletedAt IS NULL
        AND p.ean = :ean
        """)
    Optional<ProductEntity> findMerchantProByOrgIdAndEanIncludingHidden(
        @Param("orgId") Long orgId,
        @Param("ean") String ean
    );

    @Query("""
        SELECT p FROM ProductEntity p
        WHERE p.orgId = :orgId AND p.deletedAt IS NULL AND p.sourceType = 'MANUAL'
        AND LOWER(p.sku) = LOWER(:sku)
        """)
    Optional<ProductEntity> findManualByOrgIdAndSku(
        @Param("orgId") Long orgId,
        @Param("sku") String sku
    );

    @Query("""
        SELECT p FROM ProductEntity p
        WHERE p.orgId = :orgId AND p.deletedAt IS NULL AND p.sourceType = 'MANUAL'
        AND p.ean = :ean
        """)
    Optional<ProductEntity> findManualByOrgIdAndEan(
        @Param("orgId") Long orgId,
        @Param("ean") String ean
    );

    @Modifying(clearAutomatically = true)
    @Query("""
        UPDATE ProductEntity p SET p.syncStatus = 'MISSING_IN_SOURCE'
        WHERE p.orgId = :orgId AND p.sourceType = 'MERCHANTPRO' AND p.deletedAt IS NULL
        AND p.mpProductId IS NOT NULL AND p.mpProductId NOT IN :seenMpIds
        """)
    int markMissingInSourceExcept(
        @Param("orgId") Long orgId,
        @Param("seenMpIds") Collection<Long> seenMpIds
    );

    @Modifying(clearAutomatically = true)
    @Query("""
        UPDATE ProductEntity p SET p.syncStatus = 'MISSING_IN_SOURCE'
        WHERE p.orgId = :orgId AND p.sourceType = 'MERCHANTPRO' AND p.deletedAt IS NULL
        AND p.mpProductId IS NOT NULL
        """)
    int markAllMerchantProMissingInSource(@Param("orgId") Long orgId);

    @Query("""
        SELECT p FROM ProductEntity p
        WHERE p.orgId = :orgId AND p.deletedAt IS NULL AND p.hiddenAt IS NULL AND p.isActive = true
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
        WHERE p.orgId = :orgId AND p.deletedAt IS NULL AND p.hiddenAt IS NULL AND p.isActive = true
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
