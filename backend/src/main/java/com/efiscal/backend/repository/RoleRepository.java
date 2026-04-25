package com.efiscal.backend.repository;

import com.efiscal.backend.model.RoleEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<RoleEntity, Long> {
    Optional<RoleEntity> findByRoleCode(String roleCode);
    boolean existsByRoleCode(String roleCode);
}
