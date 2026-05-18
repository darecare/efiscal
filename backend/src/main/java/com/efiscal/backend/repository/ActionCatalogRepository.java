package com.efiscal.backend.repository;

import com.efiscal.backend.model.ActionCatalogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ActionCatalogRepository extends JpaRepository<ActionCatalogEntity, Long> {
    Optional<ActionCatalogEntity> findByActionCode(String actionCode);
}
