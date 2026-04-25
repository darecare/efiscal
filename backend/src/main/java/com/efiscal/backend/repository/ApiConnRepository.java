package com.efiscal.backend.repository;

import com.efiscal.backend.model.ApiConnEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApiConnRepository extends JpaRepository<ApiConnEntity, Long> {
    List<ApiConnEntity> findAllByDeletedAtIsNull();
    List<ApiConnEntity> findAllByOrgOrgIdAndDeletedAtIsNull(Long orgId);
}
