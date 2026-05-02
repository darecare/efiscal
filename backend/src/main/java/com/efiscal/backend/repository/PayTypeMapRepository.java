package com.efiscal.backend.repository;

import com.efiscal.backend.model.PayTypeMapEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PayTypeMapRepository extends JpaRepository<PayTypeMapEntity, Long> {
    List<PayTypeMapEntity> findByClientId(Long clientId);
    Optional<PayTypeMapEntity> findByClientIdAndPaymentMethodCode(Long clientId, String paymentMethodCode);
}
