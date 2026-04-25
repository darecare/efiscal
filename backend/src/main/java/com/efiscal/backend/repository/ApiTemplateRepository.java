package com.efiscal.backend.repository;

import com.efiscal.backend.model.ApiTemplateEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApiTemplateRepository extends JpaRepository<ApiTemplateEntity, Long> {
    List<ApiTemplateEntity> findAllByApiConnApiconnId(Long apiconnId);
}
