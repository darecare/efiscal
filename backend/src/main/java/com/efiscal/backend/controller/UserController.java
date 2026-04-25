package com.efiscal.backend.controller;

import com.efiscal.backend.service.DemoDataService;
import com.efiscal.backend.service.UserManagementService;
import com.efiscal.backend.service.UserManagementService.CreateUserRequest;
import com.efiscal.backend.service.UserManagementService.UpdateUserRequest;
import com.efiscal.backend.service.UserManagementService.UserDto;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserManagementService userManagementService;

    public UserController(UserManagementService userManagementService) {
        this.userManagementService = userManagementService;
    }

    @GetMapping
    public List<UserDto> listUsers() {
        return userManagementService.listUsers();
    }

    @GetMapping("/{userId}")
    public UserDto getUser(@PathVariable Long userId) {
        return userManagementService.getUser(userId);
    }

    @PostMapping
    public ResponseEntity<?> createUser(@RequestBody CreateUserRequest req) {
        if (!isSuperAdmin()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Superadmin access required");
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(userManagementService.createUser(req));
    }

    @PutMapping("/{userId}")
    public ResponseEntity<?> updateUser(@PathVariable Long userId, @RequestBody UpdateUserRequest req) {
        if (!isSuperAdmin()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Superadmin access required");
        }
        return ResponseEntity.ok(userManagementService.updateUser(userId, req));
    }

    private boolean isSuperAdmin() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        Object principal = auth.getPrincipal();
        if (principal instanceof DemoDataService.AuthenticatedUser u) {
            return "SUPERADMIN".equals(u.roleName());
        }
        return false;
    }
}
