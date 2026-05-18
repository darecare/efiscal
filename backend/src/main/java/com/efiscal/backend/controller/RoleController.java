package com.efiscal.backend.controller;

import com.efiscal.backend.service.RoleManagementService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/roles")
public class RoleController {

    private final RoleManagementService roleManagementService;

    public RoleController(RoleManagementService roleManagementService) {
        this.roleManagementService = roleManagementService;
    }

    @GetMapping
    public ResponseEntity<?> listRoles() {
        if (!hasReadAccess()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied");
        }
        return ResponseEntity.ok(roleManagementService.listRoles());
    }

    @PostMapping
    public ResponseEntity<?> createRole(@RequestBody RoleManagementService.CreateRoleRequest req) {
        if (!hasWriteAccess()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied");
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(roleManagementService.createRole(req));
    }

    private boolean hasReadAccess() {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        Object principal = auth.getPrincipal();
        if (principal instanceof com.efiscal.backend.service.DemoDataService.AuthenticatedUser u) {
            return "SUPERADMIN".equals(u.roleName()) || 
                   (u.actions() != null && (u.actions().contains("ROLES_MANAGE") || u.actions().contains("USERS_MANAGE")));
        }
        return false;
    }

    private boolean hasWriteAccess() {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        Object principal = auth.getPrincipal();
        if (principal instanceof com.efiscal.backend.service.DemoDataService.AuthenticatedUser u) {
            return "SUPERADMIN".equals(u.roleName()) || 
                   (u.actions() != null && u.actions().contains("ROLES_MANAGE"));
        }
        return false;
    }
}
