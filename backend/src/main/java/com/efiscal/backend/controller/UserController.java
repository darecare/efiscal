package com.efiscal.backend.controller;

import com.efiscal.backend.security.AuthorizationService;
import com.efiscal.backend.service.UserManagementService;
import com.efiscal.backend.service.UserManagementService.CreateUserRequest;
import com.efiscal.backend.service.UserManagementService.UpdateUserRequest;
import com.efiscal.backend.service.UserManagementService.UserDto;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserManagementService userManagementService;
    private final AuthorizationService authorizationService;

    public UserController(UserManagementService userManagementService, AuthorizationService authorizationService) {
        this.userManagementService = userManagementService;
        this.authorizationService = authorizationService;
    }

    @GetMapping
    public List<UserDto> listUsers() {
        authorizationService.requireAction("USERS_MANAGE");
        return userManagementService.listUsers(
            authorizationService.getClientId(),
            authorizationService.isSuperAdmin()
        );
    }

    @GetMapping("/{userId}")
    public UserDto getUser(@PathVariable Long userId) {
        authorizationService.requireAction("USERS_MANAGE");
        return userManagementService.getUser(
            userId,
            authorizationService.getClientId(),
            authorizationService.isSuperAdmin()
        );
    }

    @PostMapping
    public ResponseEntity<?> createUser(@Valid @RequestBody CreateUserRequest req) {
        authorizationService.requireAction("USERS_MANAGE");
        return ResponseEntity.status(HttpStatus.CREATED).body(
            userManagementService.createUser(
                req,
                authorizationService.getClientId(),
                authorizationService.isSuperAdmin()
            )
        );
    }

    @PutMapping("/{userId}")
    public ResponseEntity<?> updateUser(@PathVariable Long userId, @Valid @RequestBody UpdateUserRequest req) {
        authorizationService.requireAction("USERS_MANAGE");
        return ResponseEntity.ok(
            userManagementService.updateUser(
                userId,
                req,
                authorizationService.getClientId(),
                authorizationService.isSuperAdmin()
            )
        );
    }
}
