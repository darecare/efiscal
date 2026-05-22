package com.efiscal.backend.repository;

import com.efiscal.backend.model.RoleEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<RoleEntity, Long> {
    Optional<RoleEntity> findByRoleCodeAndClientIsNull(String roleCode);
    Optional<RoleEntity> findByRoleCodeAndClient_ClientId(String roleCode, Long clientId);
    boolean existsByRoleCode(String roleCode);
    boolean existsByRoleCodeAndClientIsNull(String roleCode);
    boolean existsByRoleCodeAndClient_ClientId(String roleCode, Long clientId);
}
