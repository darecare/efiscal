package com.efiscal.backend.repository;

import com.efiscal.backend.model.RoleActionAccessEntity;
import com.efiscal.backend.model.RoleActionAccessId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RoleActionAccessRepository extends JpaRepository<RoleActionAccessEntity, RoleActionAccessId> {
    List<RoleActionAccessEntity> findByRoleId(Long roleId);
    List<RoleActionAccessEntity> findByRoleIdIn(List<Long> roleIds);
    void deleteByRoleId(Long roleId);

    @Query("SELECT ra.action.actionCode FROM RoleActionAccessEntity ra WHERE ra.roleId = :roleId AND ra.isAllowed = true AND ra.action.isActive = true ORDER BY ra.action.actionCode")
    List<String> findActionCodesByRoleId(@Param("roleId") Long roleId);
}
