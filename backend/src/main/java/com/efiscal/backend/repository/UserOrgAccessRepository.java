package com.efiscal.backend.repository;

import com.efiscal.backend.model.UserOrgAccessEntity;
import com.efiscal.backend.model.UserOrgAccessId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserOrgAccessRepository extends JpaRepository<UserOrgAccessEntity, UserOrgAccessId> {
    List<UserOrgAccessEntity> findAllByIdUserId(Long userId);
    List<UserOrgAccessEntity> findAllByIdOrgId(Long orgId);
}
