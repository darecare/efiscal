package com.efiscal.backend.security;

import com.efiscal.backend.service.DemoDataService;
import java.util.Arrays;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthorizationService {

    public Optional<DemoDataService.AuthenticatedUser> getCurrentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return Optional.empty();
        }
        Object principal = auth.getPrincipal();
        if (principal instanceof DemoDataService.AuthenticatedUser user) {
            return Optional.of(user);
        }
        return Optional.empty();
    }

    public DemoDataService.AuthenticatedUser requireCurrentUser() {
        return getCurrentUser()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated"));
    }

    public boolean isSuperAdmin() {
        return getCurrentUser()
            .map(u -> "SUPERADMIN".equals(u.roleName()))
            .orElse(false);
    }

    public Long getClientId() {
        return getCurrentUser()
            .map(DemoDataService.AuthenticatedUser::clientId)
            .orElse(null);
    }

    public boolean hasAction(String actionCode) {
        if (isSuperAdmin()) {
            return true;
        }
        return getCurrentUser()
            .map(u -> u.actions() != null && u.actions().contains(actionCode))
            .orElse(false);
    }

    public boolean hasAnyAction(String... actionCodes) {
        if (isSuperAdmin()) {
            return true;
        }
        return getCurrentUser()
            .map(u -> u.actions() != null && Arrays.stream(actionCodes).anyMatch(u.actions()::contains))
            .orElse(false);
    }

    public void requireAction(String actionCode) {
        if (!hasAction(actionCode)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }
    }

    public void requireAnyAction(String... actionCodes) {
        if (!hasAnyAction(actionCodes)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }
    }

    public void requireSuperAdmin() {
        if (!isSuperAdmin()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Superadmin access required");
        }
    }
}
