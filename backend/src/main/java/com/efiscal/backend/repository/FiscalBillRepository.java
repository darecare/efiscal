package com.efiscal.backend.repository;

import com.efiscal.backend.model.FiscalBillEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FiscalBillRepository extends JpaRepository<FiscalBillEntity, String> {
}
