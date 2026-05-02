package com.efiscal.backend.repository;

import com.efiscal.backend.model.FiscalBillPayEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FiscalBillPayRepository extends JpaRepository<FiscalBillPayEntity, Long> {
    List<FiscalBillPayEntity> findByFiscalbillId(String fiscalbillId);
}
