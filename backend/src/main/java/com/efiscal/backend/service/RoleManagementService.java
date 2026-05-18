package com.efiscal.backend.service;

import com.efiscal.backend.model.ClientEntity;
import com.efiscal.backend.model.RoleActionAccessEntity;
import com.efiscal.backend.model.RoleEntity;
import com.efiscal.backend.repository.ActionCatalogRepository;
import com.efiscal.backend.repository.ClientRepository;
import com.efiscal.backend.repository.RoleActionAccessRepository;
import com.efiscal.backend.repository.RoleRepository;
import java.util.List;
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

    public RoleManagementService(
        RoleRepository roleRepository,
        RoleActionAccessRepository roleActionAccessRepository,
        ActionCatalogRepository actionCatalogRepository,
        ClientRepository clientRepository
    ) {
        this.roleRepository = roleRepository;
        this.roleActionAccessRepository = roleActionAccessRepository;
        this.actionCatalogRepository = actionCatalogRepository;
        this.clientRepository = clientRepository;
    }

    @Transactional(readOnly = true)
    public List<RoleDto> listRoles(Long callerClientId, boolean superAdmin) {
        return roleRepository.findAll().stream()
            .filter(RoleEntity::isActive)
            .filter(role -> superAdmin || isRoleVisibleToClient(role, callerClientId))
            .map(this::toDto)
            .toList();
    }

    @Transactional
    public RoleDto createRole(CreateRoleRequest req, Long callerClientId, boolean superAdmin) {
        validateClientScope(req.clientId(), callerClientId, superAdmin);
        assertRoleCodeAvailable(req.roleCode(), req.clientId());

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
    public RoleDto updateRole(Long roleId, UpdateRoleRequest req, Long callerClientId, boolean superAdmin) {
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
        return toDto(roleRepository.save(role));
    }

    @Transactional
    public RoleDto replaceRoleActions(Long roleId, ReplaceRoleActionsRequest req, Long callerClientId, boolean superAdmin) {
        RoleEntity role = findRoleForCaller(roleId, callerClientId, superAdmin);
        roleActionAccessRepository.deleteByRoleId(role.getRoleId());
        if (req.actionIds() != null && !req.actionIds().isEmpty()) {
            saveRoleActions(role.getRoleId(), req.actionIds());
        }
        return toDto(role);
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
            actionIds
        );
    }

    public record RoleDto(Long roleId, String roleCode, String name, String description, Long clientId, List<Long> actionIds) {}
    public record CreateRoleRequest(String roleCode, String name, String description, Long clientId, List<Long> actionIds) {}
    public record UpdateRoleRequest(String name, String description, Boolean isActive) {}
    public record ReplaceRoleActionsRequest(List<Long> actionIds) {}
}
