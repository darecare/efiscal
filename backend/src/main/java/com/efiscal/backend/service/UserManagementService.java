package com.efiscal.backend.service;

import com.efiscal.backend.model.AppUserEntity;
import com.efiscal.backend.model.ClientEntity;
import com.efiscal.backend.model.RoleEntity;
import com.efiscal.backend.repository.AppUserRepository;
import com.efiscal.backend.repository.ClientRepository;
import com.efiscal.backend.repository.RoleRepository;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UserManagementService {

    private final AppUserRepository userRepository;
    private final ClientRepository clientRepository;
    private final RoleRepository roleRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public UserManagementService(
        AppUserRepository userRepository,
        ClientRepository clientRepository,
        RoleRepository roleRepository
    ) {
        this.userRepository = userRepository;
        this.clientRepository = clientRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    @Transactional(readOnly = true)
    public List<UserDto> listUsers(Long callerClientId, boolean isSuperAdmin) {
        List<AppUserEntity> users = isSuperAdmin
            ? userRepository.findAllByDeletedAtIsNull()
            : userRepository.findAllByClientClientIdAndDeletedAtIsNull(callerClientId);
        return users.stream()
            .map(this::toDto)
            .toList();
    }

    @Transactional(readOnly = true)
    public UserDto getUser(Long userId, Long callerClientId, boolean isSuperAdmin) {
        AppUserEntity user = userRepository.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        if (!isSuperAdmin && !user.getClient().getClientId().equals(callerClientId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }
        return toDto(user);
    }

    @Transactional
    public UserDto createUser(CreateUserRequest req, Long callerClientId, boolean isSuperAdmin) {
        if (!isSuperAdmin && (req.clientId() == null || !req.clientId().equals(callerClientId))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }
        if (req.clientId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Client ID must not be null");
        }
        if (userRepository.existsByEmail(req.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already in use");
        }
        ClientEntity client = clientRepository.findById(req.clientId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Client not found"));
        RoleEntity role = roleRepository.findById(req.roleId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Role not found"));

        validateRoleScope(role, req.clientId(), isSuperAdmin);

        AppUserEntity user = new AppUserEntity();
        user.setEmail(req.email());
        user.setPasswordHash(passwordEncoder.encode(req.password()));
        user.setFullName(req.fullName());
        user.setClient(client);
        user.setRole(role);
        user.setSubscriptionStatus(req.subscriptionStatus() != null ? req.subscriptionStatus() : "ACTIVE");
        user.setSubscriptionStartAt(req.subscriptionStartAt());
        user.setSubscriptionExpiresAt(req.subscriptionExpiresAt());
        user.setActive(true);
        return toDto(userRepository.save(user));
    }

    @Transactional
    public UserDto updateUser(Long userId, UpdateUserRequest req, Long callerClientId, boolean isSuperAdmin) {
        AppUserEntity user = userRepository.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (!isSuperAdmin && !user.getClient().getClientId().equals(callerClientId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        if (req.fullName() != null) user.setFullName(req.fullName());
        if (req.subscriptionStatus() != null) user.setSubscriptionStatus(req.subscriptionStatus());
        if (req.subscriptionStartAt() != null) user.setSubscriptionStartAt(req.subscriptionStartAt());
        if (req.subscriptionExpiresAt() != null) user.setSubscriptionExpiresAt(req.subscriptionExpiresAt());
        if (req.isActive() != null) user.setActive(req.isActive());
        if (req.newPassword() != null && !req.newPassword().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(req.newPassword()));
        }

        Long targetClientId = user.getClient().getClientId();
        if (req.clientId() != null) {
            if (!isSuperAdmin && !req.clientId().equals(callerClientId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot assign user to another client");
            }
            ClientEntity client = clientRepository.findById(req.clientId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Client not found"));
            user.setClient(client);
            targetClientId = client.getClientId();
        }

        if (req.roleId() != null) {
            RoleEntity role = roleRepository.findById(req.roleId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Role not found"));
            validateRoleScope(role, targetClientId, isSuperAdmin);
            user.setRole(role);
        }

        return toDto(userRepository.save(user));
    }

    private void validateRoleScope(RoleEntity role, Long targetClientId, boolean isSuperAdmin) {
        if (role.getClient() != null && !role.getClient().getClientId().equals(targetClientId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot assign a role belonging to another client");
        }
        if ("SUPERADMIN".equals(role.getRoleCode()) && !isSuperAdmin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only superadmins can assign the SUPERADMIN role");
        }
    }

    private UserDto toDto(AppUserEntity u) {
        return new UserDto(
            u.getUserId(),
            u.getEmail(),
            u.getFullName(),
            u.getRole().getRoleCode(),
            u.getRole().getName(),
            u.getRole().getRoleId(),
            u.getClient().getClientId(),
            u.getClient().getName(),
            u.getSubscriptionStatus(),
            u.getSubscriptionStartAt(),
            u.getSubscriptionExpiresAt(),
            u.isActive()
        );
    }

    public record UserDto(
        Long userId,
        String email,
        String fullName,
        String roleCode,
        String roleName,
        Long roleId,
        Long clientId,
        String clientName,
        String subscriptionStatus,
        OffsetDateTime subscriptionStartAt,
        OffsetDateTime subscriptionExpiresAt,
        boolean isActive
    ) {}

    public record CreateUserRequest(
        String email,
        String password,
        String fullName,
        Long clientId,
        Long roleId,
        String subscriptionStatus,
        OffsetDateTime subscriptionStartAt,
        OffsetDateTime subscriptionExpiresAt
    ) {}

    public record UpdateUserRequest(
        String fullName,
        Long roleId,
        Long clientId,
        String subscriptionStatus,
        OffsetDateTime subscriptionStartAt,
        OffsetDateTime subscriptionExpiresAt,
        Boolean isActive,
        String newPassword
    ) {}
}
