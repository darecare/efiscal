package com.efiscal.backend.service;

import com.efiscal.backend.model.AppUserEntity;
import com.efiscal.backend.model.ClientEntity;
import com.efiscal.backend.model.RoleEntity;
import com.efiscal.backend.model.OrgEntity;
import com.efiscal.backend.model.UserOrgAccessEntity;
import com.efiscal.backend.model.UserOrgAccessId;
import com.efiscal.backend.repository.AppUserRepository;
import com.efiscal.backend.repository.ClientRepository;
import com.efiscal.backend.repository.RoleRepository;
import com.efiscal.backend.repository.OrgRepository;
import com.efiscal.backend.repository.UserOrgAccessRepository;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
    private final OrgRepository orgRepository;
    private final UserOrgAccessRepository userOrgAccessRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public UserManagementService(
        AppUserRepository userRepository,
        ClientRepository clientRepository,
        RoleRepository roleRepository,
        OrgRepository orgRepository,
        UserOrgAccessRepository userOrgAccessRepository
    ) {
        this.userRepository = userRepository;
        this.clientRepository = clientRepository;
        this.roleRepository = roleRepository;
        this.orgRepository = orgRepository;
        this.userOrgAccessRepository = userOrgAccessRepository;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    @Transactional(readOnly = true)
    public List<UserDto> listUsers(Long callerClientId, boolean isSuperAdmin) {
        if (!isSuperAdmin && callerClientId == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: client scope not resolved");
        }
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
        AppUserEntity savedUser = userRepository.save(user);

        if (req.orgIds() != null && !req.orgIds().isEmpty()) {
            saveUserOrgs(savedUser, req.orgIds(), req.clientId(), isSuperAdmin);
        }
        return toDto(savedUser);
    }

    private void saveUserOrgs(AppUserEntity user, List<Long> orgIds, Long targetClientId, boolean isSuperAdmin) {
        for (Long orgId : orgIds) {
            OrgEntity org = orgRepository.findById(orgId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Organization not found: " + orgId));
            if (!isSuperAdmin && !org.getClient().getClientId().equals(targetClientId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Organization " + orgId + " is outside user client scope");
            }
            UserOrgAccessEntity access = new UserOrgAccessEntity();
            access.setId(new UserOrgAccessId(user.getUserId(), orgId));
            access.setUser(user);
            access.setOrg(org);
            access.setActive(true);
            userOrgAccessRepository.save(access);
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

        AppUserEntity savedUser = userRepository.save(user);

        if (req.orgIds() != null) {
            userOrgAccessRepository.deleteByUserId(savedUser.getUserId());
            if (!req.orgIds().isEmpty()) {
                saveUserOrgs(savedUser, req.orgIds(), targetClientId, isSuperAdmin);
            }
        }

        return toDto(savedUser);
    }

    @Transactional
    public void deleteUser(Long userId, Long callerClientId, boolean isSuperAdmin, String callerUserId) {
        AppUserEntity user = userRepository.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (!isSuperAdmin && !user.getClient().getClientId().equals(callerClientId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }
        
        if (callerUserId != null && callerUserId.equals(String.valueOf(user.getUserId()))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot delete your own account");
        }

        user.setDeletedAt(OffsetDateTime.now());
        userRepository.save(user);
        userOrgAccessRepository.deleteByUserId(user.getUserId());
    }

    private void validateRoleScope(RoleEntity role, Long targetClientId, boolean isSuperAdmin) {
        if (role.getClient() != null && !role.getClient().getClientId().equals(targetClientId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot assign a role belonging to another client");
        }
        if (RoleEntity.ROLE_SUPERADMIN.equals(role.getRoleCode()) && !isSuperAdmin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only superadmins can assign the " + RoleEntity.ROLE_SUPERADMIN + " role");
        }
    }

    private UserDto toDto(AppUserEntity u) {
        List<Long> orgIds = userOrgAccessRepository.findAllByIdUserId(u.getUserId()).stream()
            .map(access -> access.getId().getOrgId())
            .toList();
        return new UserDto(
            u.getUserId(),
            u.getEmail(),
            u.getFullName(),
            u.getRole() != null ? u.getRole().getRoleCode() : null,
            u.getRole() != null ? u.getRole().getName() : null,
            u.getRole() != null ? u.getRole().getRoleId() : null,
            u.getClient() != null ? u.getClient().getClientId() : null,
            u.getClient() != null ? u.getClient().getName() : null,
            u.getSubscriptionStatus(),
            u.getSubscriptionStartAt(),
            u.getSubscriptionExpiresAt(),
            u.isActive(),
            orgIds
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
        boolean isActive,
        List<Long> orgIds
    ) {}

    public record CreateUserRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        @Size(max = 255, message = "Email must not exceed 255 characters")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 6, max = 100, message = "Password must be between 6 and 100 characters")
        String password,

        @NotBlank(message = "Full name is required")
        @Size(max = 255, message = "Full name must not exceed 255 characters")
        String fullName,

        @NotNull(message = "Client ID is required")
        Long clientId,

        @NotNull(message = "Role ID is required")
        Long roleId,

        @Size(max = 30, message = "Subscription status must not exceed 30 characters")
        String subscriptionStatus,

        OffsetDateTime subscriptionStartAt,
        OffsetDateTime subscriptionExpiresAt,
        List<Long> orgIds
    ) {}

    public record UpdateUserRequest(
        @Size(max = 255, message = "Full name must not exceed 255 characters")
        String fullName,

        Long roleId,
        Long clientId,

        @Size(max = 30, message = "Subscription status must not exceed 30 characters")
        String subscriptionStatus,

        OffsetDateTime subscriptionStartAt,
        OffsetDateTime subscriptionExpiresAt,
        Boolean isActive,

        @Size(max = 100, message = "Password must not exceed 100 characters")
        String newPassword,
        List<Long> orgIds
    ) {}
}
