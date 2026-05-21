package com.efiscal.backend.controller;

import com.efiscal.backend.security.AuthorizationService;
import com.efiscal.backend.service.RoleManagementService;
import com.efiscal.backend.service.RoleManagementService.CreateRoleRequest;
import com.efiscal.backend.service.RoleManagementService.ReplaceRoleActionsRequest;
import com.efiscal.backend.service.RoleManagementService.UpdateRoleRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/roles")
public class RoleController {

    private final RoleManagementService roleManagementService;
    private final AuthorizationService authorizationService;

    public RoleController(RoleManagementService roleManagementService, AuthorizationService authorizationService) {
        this.roleManagementService = roleManagementService;
        this.authorizationService = authorizationService;
    }

    @GetMapping
    public ResponseEntity<?> listRoles(@RequestParam(required = false, defaultValue = "false") boolean includeInactive) {
        authorizationService.requireAnyAction("ROLES_MANAGE", "USERS_MANAGE");
        return ResponseEntity.ok(roleManagementService.listRoles(
            authorizationService.getClientId(),
            authorizationService.isSuperAdmin(),
            includeInactive));
    }

    @PostMapping
    public ResponseEntity<?> createRole(@RequestBody CreateRoleRequest req) {
        authorizationService.requireAction("ROLES_MANAGE");
        java.util.List<String> callerActions = authorizationService.getCurrentUser()
            .map(com.efiscal.backend.service.DemoDataService.AuthenticatedUser::actions)
            .orElse(java.util.List.of());
        return ResponseEntity.status(HttpStatus.CREATED).body(roleManagementService.createRole(
            req,
            authorizationService.getClientId(),
            authorizationService.isSuperAdmin(),
            callerActions));
    }

    @PutMapping("/{roleId}")
    public ResponseEntity<?> updateRole(@PathVariable Long roleId, @RequestBody UpdateRoleRequest req) {
        authorizationService.requireAction("ROLES_MANAGE");
        return ResponseEntity.ok(roleManagementService.updateRole(
            roleId,
            req,
            authorizationService.getClientId(),
            authorizationService.isSuperAdmin()));
    }

    @PutMapping("/{roleId}/actions")
    public ResponseEntity<?> replaceRoleActions(
        @PathVariable Long roleId,
        @RequestBody ReplaceRoleActionsRequest req
    ) {
        authorizationService.requireAction("ROLES_MANAGE");
        java.util.List<String> callerActions = authorizationService.getCurrentUser()
            .map(com.efiscal.backend.service.DemoDataService.AuthenticatedUser::actions)
            .orElse(java.util.List.of());
        return ResponseEntity.ok(roleManagementService.replaceRoleActions(
            roleId,
            req,
            authorizationService.getClientId(),
            authorizationService.isSuperAdmin(),
            callerActions));
    }
}
