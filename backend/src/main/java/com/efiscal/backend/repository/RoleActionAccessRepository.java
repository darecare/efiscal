package com.efiscal.backend.repository;

import com.efiscal.backend.model.RoleActionAccessEntity;
import com.efiscal.backend.model.RoleActionAccessId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RoleActionAccessRepository extends JpaRepository<RoleActionAccessEntity, RoleActionAccessId> {
    List<RoleActionAccessEntity> findByRoleId(Long roleId);
    void deleteByRoleId(Long roleId);
}
