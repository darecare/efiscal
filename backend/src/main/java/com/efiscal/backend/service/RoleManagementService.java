package com.efiscal.backend.service;

import com.efiscal.backend.model.ClientEntity;
import com.efiscal.backend.model.RoleActionAccessEntity;
import com.efiscal.backend.model.RoleEntity;
import com.efiscal.backend.repository.ActionCatalogRepository;
import com.efiscal.backend.repository.ClientRepository;
import com.efiscal.backend.repository.RoleActionAccessRepository;
import com.efiscal.backend.repository.RoleRepository;
import com.efiscal.backend.repository.AppUserRepository;
import com.efiscal.backend.security.RolePermissionService;
import java.util.List;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class RoleManagementService {

    private final RoleRepository roleRepository;
    private final RoleActionAccessRepository roleActionAccessRepository;
    private final ActionCatalogRepository actionCatalogRepository;
    private final ClientRepository clientRepository;
    private final AppUserRepository appUserRepository;
    private final RolePermissionService rolePermissionService;

    public RoleManagementService(
        RoleRepository roleRepository,
        RoleActionAccessRepository roleActionAccessRepository,
        ActionCatalogRepository actionCatalogRepository,
        ClientRepository clientRepository,
        AppUserRepository appUserRepository,
        RolePermissionService rolePermissionService
    ) {
        this.roleRepository = roleRepository;
        this.roleActionAccessRepository = roleActionAccessRepository;
        this.actionCatalogRepository = actionCatalogRepository;
        this.clientRepository = clientRepository;
        this.appUserRepository = appUserRepository;
        this.rolePermissionService = rolePermissionService;
    }

    @Transactional(readOnly = true)
    public List<RoleDto> listRoles(Long callerClientId, boolean superAdmin, boolean includeInactive) {
        List<RoleEntity> roles = roleRepository.findAll();
        List<Long> roleIds = roles.stream().map(RoleEntity::getRoleId).toList();
        List<RoleActionAccessEntity> allAccess = roleActionAccessRepository.findByRoleIdIn(roleIds);
        
        java.util.Map<Long, List<Long>> actionsByRoleId = new java.util.HashMap<>();
        for (RoleActionAccessEntity access : allAccess) {
            if (access.isAllowed()) {
                actionsByRoleId.computeIfAbsent(access.getRoleId(), k -> new java.util.ArrayList<>())
                    .add(access.getActionId());
            }
        }

        return roles.stream()
            .filter(role -> includeInactive || role.isActive())
            .filter(role -> superAdmin || isRoleVisibleToClient(role, callerClientId))
            .map(r -> new RoleDto(
                r.getRoleId(),
                r.getRoleCode(),
                r.getName(),
                r.getDescription(),
                r.getClient() != null ? r.getClient().getClientId() : null,
                actionsByRoleId.getOrDefault(r.getRoleId(), List.of()),
                r.isActive()
            ))
            .toList();
    }

    @Transactional
    public RoleDto createRole(CreateRoleRequest req, Long callerClientId, boolean superAdmin, String callerUserId, List<String> callerActions) {
        validateClientScope(req.clientId(), callerClientId, superAdmin);
        assertRoleCodeAvailable(req.roleCode(), req.clientId());
        if (req.actionIds() != null) {
            validateRoleActions(req.actionIds(), getFreshCallerActions(callerUserId, superAdmin, callerActions), superAdmin);
        }

        RoleEntity role = new RoleEntity();
        role.setRoleCode(req.roleCode());
        role.setName(req.name());
        role.setDescription(req.description());

        if (req.clientId() != null) {
            ClientEntity client = clientRepository.findById(req.clientId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Client not found"));
            role.setClient(client);
        }

        role.setActive(true);
        role = roleRepository.save(role);

        if (req.actionIds() != null) {
            saveRoleActions(role.getRoleId(), req.actionIds());
        }
        return toDto(role);
    }

    @Transactional
    public RoleDto updateRole(Long roleId, UpdateRoleRequest req, Long callerClientId, boolean superAdmin, String callerUserId, List<String> callerActions) {
        RoleEntity role = findRoleForCaller(roleId, callerClientId, superAdmin);
        if (req.name() != null) {
            role.setName(req.name());
        }
        if (req.description() != null) {
            role.setDescription(req.description());
        }
        if (req.isActive() != null) {
            role.setActive(req.isActive());
        }
        role = roleRepository.save(role);

        if (req.actionIds() != null) {
            validateRoleActions(req.actionIds(), getFreshCallerActions(callerUserId, superAdmin, callerActions), superAdmin);
            roleActionAccessRepository.deleteByRoleId(role.getRoleId());
            if (!req.actionIds().isEmpty()) {
                saveRoleActions(role.getRoleId(), req.actionIds());
            }
        }
        return toDto(role);
    }

    @Transactional
    public RoleDto replaceRoleActions(Long roleId, ReplaceRoleActionsRequest req, Long callerClientId, boolean superAdmin, String callerUserId, List<String> callerActions) {
        RoleEntity role = findRoleForCaller(roleId, callerClientId, superAdmin);
        if (req.actionIds() != null && !req.actionIds().isEmpty()) {
            validateRoleActions(req.actionIds(), getFreshCallerActions(callerUserId, superAdmin, callerActions), superAdmin);
        }
        roleActionAccessRepository.deleteByRoleId(role.getRoleId());
        if (req.actionIds() != null && !req.actionIds().isEmpty()) {
            saveRoleActions(role.getRoleId(), req.actionIds());
        }
        return toDto(role);
    }

    private List<String> getFreshCallerActions(String callerUserId, boolean superAdmin, List<String> sessionActions) {
        if (superAdmin) {
            return List.of();
        }
        if (callerUserId == null) {
            return sessionActions != null ? sessionActions : List.of();
        }
        try {
            Long userId = Long.parseLong(callerUserId);
            return appUserRepository.findById(userId)
                .map(user -> rolePermissionService.resolveActionCodes(user.getRole()))
                .orElse(sessionActions != null ? sessionActions : List.of());
        } catch (NumberFormatException e) {
            return sessionActions != null ? sessionActions : List.of();
        }
    }

    private void validateRoleActions(List<Long> actionIds, List<String> callerActions, boolean superAdmin) {
        if (superAdmin) return;
        for (Long actionId : actionIds) {
            com.efiscal.backend.model.ActionCatalogEntity action = actionCatalogRepository.findById(actionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Action ID " + actionId + " not found"));
            if (callerActions == null || !callerActions.contains(action.getActionCode())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, 
                    "Cannot assign permission '" + action.getActionCode() + "' which you do not possess.");
            }
        }
    }

    private RoleEntity findRoleForCaller(Long roleId, Long callerClientId, boolean superAdmin) {
        RoleEntity role = roleRepository.findById(roleId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Role not found"));
        if (!superAdmin) {
            if (role.getClient() == null || !isRoleVisibleToClient(role, callerClientId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
            }
        }
        return role;
    }

    private static boolean isRoleVisibleToClient(RoleEntity role, Long callerClientId) {
        if (role.getClient() == null) {
            return true;
        }
        return callerClientId != null
            && role.getClient().getClientId() != null
            && role.getClient().getClientId().equals(callerClientId);
    }

    private void validateClientScope(Long requestedClientId, Long callerClientId, boolean superAdmin) {
        if (superAdmin) {
            return;
        }
        if (requestedClientId == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only superadmin can create global roles");
        }
        if (callerClientId == null || !callerClientId.equals(requestedClientId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot assign role outside your client scope");
        }
    }

    private void assertRoleCodeAvailable(String roleCode, Long clientId) {
        boolean exists = clientId == null
            ? roleRepository.existsByRoleCodeAndClientIsNull(roleCode)
            : roleRepository.existsByRoleCodeAndClient_ClientId(roleCode, clientId);
        if (exists) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Role code already exists for this scope");
        }
    }

    private void saveRoleActions(Long roleId, List<Long> actionIds) {
        for (Long actionId : actionIds) {
            actionCatalogRepository.findById(actionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Action ID " + actionId + " not found"));
            RoleActionAccessEntity access = new RoleActionAccessEntity();
            access.setRoleId(roleId);
            access.setActionId(actionId);
            access.setAllowed(true);
            roleActionAccessRepository.save(access);
        }
    }

    private RoleDto toDto(RoleEntity r) {
        List<Long> actionIds = roleActionAccessRepository.findByRoleId(r.getRoleId()).stream()
            .filter(RoleActionAccessEntity::isAllowed)
            .map(RoleActionAccessEntity::getActionId)
            .toList();
        return new RoleDto(
            r.getRoleId(),
            r.getRoleCode(),
            r.getName(),
            r.getDescription(),
            r.getClient() != null ? r.getClient().getClientId() : null,
            actionIds,
            r.isActive()
        );
    }

    public record RoleDto(Long roleId, String roleCode, String name, String description, Long clientId, List<Long> actionIds, Boolean isActive) {}
    public record CreateRoleRequest(
        @NotBlank(message = "Role code is required")
        @Size(max = 50, message = "Role code must not exceed 50 characters")
        String roleCode,

        @NotBlank(message = "Role name is required")
        @Size(max = 100, message = "Role name must not exceed 100 characters")
        String name,

        @Size(max = 255, message = "Description must not exceed 255 characters")
        String description,

        Long clientId,
        List<Long> actionIds
    ) {}
    public record UpdateRoleRequest(
        @Size(max = 100, message = "Role name must not exceed 100 characters")
        String name,

        @Size(max = 255, message = "Description must not exceed 255 characters")
        String description,

        Boolean isActive,
        List<Long> actionIds
    ) {}
    public record ReplaceRoleActionsRequest(List<Long> actionIds) {}
}
