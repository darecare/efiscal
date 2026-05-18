package com.efiscal.backend.controller;

import com.efiscal.backend.security.AuthorizationService;
import com.efiscal.backend.service.UserManagementService;
import com.efiscal.backend.service.UserManagementService.CreateUserRequest;
import com.efiscal.backend.service.UserManagementService.UpdateUserRequest;
import com.efiscal.backend.service.UserManagementService.UserDto;
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
        return userManagementService.listUsers();
    }

    @GetMapping("/{userId}")
    public UserDto getUser(@PathVariable Long userId) {
        authorizationService.requireAction("USERS_MANAGE");
        return userManagementService.getUser(userId);
    }

    @PostMapping
    public ResponseEntity<?> createUser(@RequestBody CreateUserRequest req) {
        authorizationService.requireAction("USERS_MANAGE");
        return ResponseEntity.status(HttpStatus.CREATED).body(userManagementService.createUser(req));
    }

    @PutMapping("/{userId}")
    public ResponseEntity<?> updateUser(@PathVariable Long userId, @RequestBody UpdateUserRequest req) {
        authorizationService.requireAction("USERS_MANAGE");
        return ResponseEntity.ok(userManagementService.updateUser(userId, req));
    }
}
