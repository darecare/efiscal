package com.efiscal.backend.repository;

import com.efiscal.backend.model.FiscalBillConfigEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FiscalBillConfigRepository extends JpaRepository<FiscalBillConfigEntity, Long> {
    Optional<FiscalBillConfigEntity> findFirstByOrgIdAndIsactive(Long orgId, String isactive);
}
