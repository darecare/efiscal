package com.efiscal.backend.repository;

import com.efiscal.backend.model.FiscalBillIdempotencyKeyEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FiscalBillIdempotencyKeyRepository extends JpaRepository<FiscalBillIdempotencyKeyEntity, String> {
}
