package com.efiscal.backend.controller;

import com.efiscal.backend.service.DemoDataService;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final DemoDataService demoDataService;

    public AuthController(DemoDataService demoDataService) {
        this.demoDataService = demoDataService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        DemoDataService.LoginResult result = demoDataService.login(request.email(), request.password());
        if (result == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse("Invalid email or password"));
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/me")
    public DemoDataService.AuthenticatedUser me(@AuthenticationPrincipal DemoDataService.AuthenticatedUser user) {
        return user;
    }

    public record LoginRequest(@Email @NotBlank String email, @NotBlank String password) {
    }

    public record ErrorResponse(String message) {
    }
}
