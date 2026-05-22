package com.efiscal.backend.repository;

import com.efiscal.backend.model.ActionCatalogEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ActionCatalogRepository extends JpaRepository<ActionCatalogEntity, Long> {
    Optional<ActionCatalogEntity> findByActionCode(String actionCode);
    List<ActionCatalogEntity> findByModuleCodeAndIsActiveTrue(String moduleCode);
}
