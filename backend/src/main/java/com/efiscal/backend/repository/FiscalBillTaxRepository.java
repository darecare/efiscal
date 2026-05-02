package com.efiscal.backend.repository;

import com.efiscal.backend.model.FiscalBillTaxEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FiscalBillTaxRepository extends JpaRepository<FiscalBillTaxEntity, Long> {
    List<FiscalBillTaxEntity> findByFiscalbillId(Long fiscalbillId);
}
