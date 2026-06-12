package com.efiscal.backend.repository;

import com.efiscal.backend.model.EmailTemplateEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailTemplateRepository extends JpaRepository<EmailTemplateEntity, Long> {
    List<EmailTemplateEntity> findAllByOrgOrgIdAndDeletedAtIsNull(Long orgId);
    Optional<EmailTemplateEntity> findTopByOrgOrgIdAndDeletedAtIsNullAndIsActiveTrueOrderByUpdatedAtDesc(Long orgId);
}