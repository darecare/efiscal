package com.efiscal.backend.repository;

import com.efiscal.backend.model.AppUserEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppUserRepository extends JpaRepository<AppUserEntity, Long> {
    List<AppUserEntity> findAllByDeletedAtIsNull();
    List<AppUserEntity> findAllByClientClientIdAndDeletedAtIsNull(Long clientId);
    Optional<AppUserEntity> findByEmail(String email);
    boolean existsByEmail(String email);

    long countByRoleRoleIdAndDeletedAtIsNull(Long roleId);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("UPDATE AppUserEntity u SET u.role = :newRole WHERE u.role.roleId = :oldRoleId AND u.deletedAt IS NULL")
    int updateRoleForUsers(Long oldRoleId, com.efiscal.backend.model.RoleEntity newRole);
}
