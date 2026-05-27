package com.efiscal.backend.security;

import com.efiscal.backend.model.ActionCatalogEntity;
import com.efiscal.backend.model.RoleActionAccessEntity;
import com.efiscal.backend.model.RoleEntity;
import com.efiscal.backend.repository.ActionCatalogRepository;
import com.efiscal.backend.repository.RoleActionAccessRepository;
import com.efiscal.backend.repository.RoleRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RolePermissionService {

    private final RoleRepository roleRepository;
    private final RoleActionAccessRepository roleActionAccessRepository;
    private final ActionCatalogRepository actionCatalogRepository;

    public RolePermissionService(
        RoleRepository roleRepository,
        RoleActionAccessRepository roleActionAccessRepository,
        ActionCatalogRepository actionCatalogRepository
    ) {
        this.roleRepository = roleRepository;
        this.roleActionAccessRepository = roleActionAccessRepository;
        this.actionCatalogRepository = actionCatalogRepository;
    }

    @Transactional(readOnly = true)
    public List<String> resolveActionCodes(String roleCode) {
        return roleRepository.findByRoleCodeAndClientIsNull(roleCode)
            .map(role -> resolveActionCodes(role.getRoleId()))
            .orElse(List.of());
    }

    @Transactional(readOnly = true)
    public List<String> resolveActionCodes(String roleCode, Long clientId) {
        if (clientId == null) {
            return resolveActionCodes(roleCode);
        }
        return roleRepository.findByRoleCodeAndClient_ClientId(roleCode, clientId)
            .map(role -> resolveActionCodes(role.getRoleId()))
            .or(() -> roleRepository.findByRoleCodeAndClientIsNull(roleCode)
                .map(role -> resolveActionCodes(role.getRoleId())))
            .orElse(List.of());
    }

    @Transactional(readOnly = true)
    public List<String> resolveActionCodes(Long roleId) {
        if (roleId == null) {
            return List.of();
        }
        return roleActionAccessRepository.findActionCodesByRoleId(roleId);
    }

    @Transactional(readOnly = true)
    public List<String> resolveActionCodes(RoleEntity role) {
        if (role == null || role.getRoleId() == null) {
            return List.of();
        }
        return resolveActionCodes(role.getRoleId());
    }
}
