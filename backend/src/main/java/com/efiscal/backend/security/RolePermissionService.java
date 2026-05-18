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
            .or(() -> roleRepository.findByRoleCode(roleCode))
            .map(role -> resolveActionCodes(role.getRoleId()))
            .orElse(List.of());
    }

    @Transactional(readOnly = true)
    public List<String> resolveActionCodes(Long roleId) {
        List<RoleActionAccessEntity> accessRows = roleActionAccessRepository.findByRoleId(roleId);
        if (accessRows.isEmpty()) {
            return List.of();
        }
        return accessRows.stream()
            .filter(RoleActionAccessEntity::isAllowed)
            .map(RoleActionAccessEntity::getActionId)
            .distinct()
            .map(actionId -> actionCatalogRepository.findById(actionId).orElse(null))
            .filter(action -> action != null && action.isActive())
            .map(ActionCatalogEntity::getActionCode)
            .sorted()
            .toList();
    }

    @Transactional(readOnly = true)
    public List<String> resolveActionCodes(RoleEntity role) {
        if (role == null || role.getRoleId() == null) {
            return List.of();
        }
        return resolveActionCodes(role.getRoleId());
    }
}
