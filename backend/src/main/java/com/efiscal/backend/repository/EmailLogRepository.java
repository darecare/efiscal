package com.efiscal.backend.repository;

import com.efiscal.backend.model.EmailLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailLogRepository extends JpaRepository<EmailLogEntity, Long> {
}
