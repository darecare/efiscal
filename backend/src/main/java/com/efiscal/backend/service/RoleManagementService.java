package com.efiscal.backend.service;

import com.efiscal.backend.model.ClientEntity;
import com.efiscal.backend.model.RoleActionAccessEntity;
import com.efiscal.backend.model.RoleEntity;
import com.efiscal.backend.repository.ActionCatalogRepository;
import com.efiscal.backend.repository.ClientRepository;
import com.efiscal.backend.repository.RoleActionAccessRepository;
import com.efiscal.backend.repository.RoleRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class RoleManagementService {

    private final RoleRepository roleRepository;
    private final RoleActionAccessRepository roleActionAccessRepository;
    private final ActionCatalogRepository actionCatalogRepository;
    private final ClientRepository clientRepository;

    public RoleManagementService(RoleRepository roleRepository, RoleActionAccessRepository roleActionAccessRepository, ActionCatalogRepository actionCatalogRepository, ClientRepository clientRepository) {
        this.roleRepository = roleRepository;
        this.roleActionAccessRepository = roleActionAccessRepository;
        this.actionCatalogRepository = actionCatalogRepository;
        this.clientRepository = clientRepository;
    }

    @Transactional(readOnly = true)
    public List<RoleDto> listRoles() {
        return roleRepository.findAll().stream()
                .filter(RoleEntity::isActive)
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public RoleDto createRole(CreateRoleRequest req) {
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
            for (Long actionId : req.actionIds()) {
                actionCatalogRepository.findById(actionId)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Action ID " + actionId + " not found"));
                RoleActionAccessEntity access = new RoleActionAccessEntity();
                access.setRoleId(role.getRoleId());
                access.setActionId(actionId);
                access.setAllowed(true);
                roleActionAccessRepository.save(access);
            }
        }
        return toDto(role);
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
}
