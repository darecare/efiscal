package com.efiscal.backend.repository;

import com.efiscal.backend.model.ProductSyncJobEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductSyncJobRepository extends JpaRepository<ProductSyncJobEntity, Long> {

    Optional<ProductSyncJobEntity> findByOrgIdAndStatus(Long orgId, String status);

    Optional<ProductSyncJobEntity> findTopByOrgIdAndSyncTypeAndStatusOrderByStartedAtDesc(
        Long orgId,
        String syncType,
        String status
    );

    Optional<ProductSyncJobEntity> findTopByOrgIdOrderByStartedAtDesc(Long orgId);
}
