package com.efiscal.backend.repository;

import com.efiscal.backend.model.FiscalBillLineEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FiscalBillLineRepository extends JpaRepository<FiscalBillLineEntity, Long> {
    List<FiscalBillLineEntity> findByFiscalbillId(Long fiscalbillId);
}
