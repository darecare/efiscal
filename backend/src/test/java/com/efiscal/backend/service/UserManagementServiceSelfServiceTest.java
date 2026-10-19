package com.efiscal.backend.service;

import com.efiscal.backend.model.AppUserEntity;
import com.efiscal.backend.repository.AppUserRepository;
import com.efiscal.backend.repository.ClientRepository;
import com.efiscal.backend.repository.OrgRepository;
import com.efiscal.backend.repository.RoleRepository;
import com.efiscal.backend.repository.UserOrgAccessRepository;
import com.efiscal.backend.service.UserManagementService.ChangeMyPasswordRequest;
import com.efiscal.backend.service.UserManagementService.UpdateMyProfileRequest;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UserManagementServiceSelfServiceTest {

    private static final Long USER_ID = 42L;

    @Mock AppUserRepository userRepository;
    @Mock ClientRepository clientRepository;
    @Mock RoleRepository roleRepository;
    @Mock OrgRepository orgRepository;
    @Mock UserOrgAccessRepository userOrgAccessRepository;

    private UserManagementService service;
    private AppUserEntity user;

    @BeforeEach
    void setUp() {
        service = new UserManagementService(
            userRepository, clientRepository, roleRepository, orgRepository, userOrgAccessRepository);
        user = new AppUserEntity();
        user.setUserId(USER_ID);
        user.setEmail("old@example.com");
        user.setFullName("Old Name");
        user.setPasswordHash("old-hash");
        user.setActive(true);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userRepository.save(any(AppUserEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userOrgAccessRepository.findAllByIdUserId(USER_ID)).thenReturn(List.of());
    }

    @Test
    void updateMyProfile_updatesTrimmedNameAndEmail() {
        when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());

        var dto = service.updateMyProfile(USER_ID, new UpdateMyProfileRequest("  New Name ", " new@example.com "));

        assertEquals("New Name", dto.fullName());
        assertEquals("new@example.com", dto.email());
        assertEquals("new@example.com", user.getEmail());
    }

    @Test
    void updateMyProfile_keepingOwnEmailSkipsConflictCheck() {
        var dto = service.updateMyProfile(USER_ID, new UpdateMyProfileRequest("Renamed", "old@example.com"));

        assertEquals("Renamed", dto.fullName());
        verify(userRepository, never()).findByEmail(any());
    }

    @Test
    void updateMyProfile_rejectsEmailUsedByAnotherUser() {
        var other = new AppUserEntity();
        other.setUserId(99L);
        when(userRepository.findByEmail("taken@example.com")).thenReturn(Optional.of(other));

        var ex = assertThrows(ResponseStatusException.class, () ->
            service.updateMyProfile(USER_ID, new UpdateMyProfileRequest("Name", "taken@example.com")));

        assertEquals(409, ex.getStatusCode().value());
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateMyProfile_rejectsBlankNameAndInvalidEmail() {
        var blankName = assertThrows(ResponseStatusException.class, () ->
            service.updateMyProfile(USER_ID, new UpdateMyProfileRequest("  ", "a@b.rs")));
        var badEmail = assertThrows(ResponseStatusException.class, () ->
            service.updateMyProfile(USER_ID, new UpdateMyProfileRequest("Name", "not-an-email")));

        assertEquals(400, blankName.getStatusCode().value());
        assertEquals(400, badEmail.getStatusCode().value());
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateMyProfile_rejectsDeletedUser() {
        user.setDeletedAt(OffsetDateTime.now());

        var ex = assertThrows(ResponseStatusException.class, () ->
            service.updateMyProfile(USER_ID, new UpdateMyProfileRequest("Name", "a@b.rs")));

        assertEquals(404, ex.getStatusCode().value());
    }

    @Test
    void changeMyPassword_storesBcryptHashOfNewPassword() {
        service.changeMyPassword(USER_ID, new ChangeMyPasswordRequest("secret123", "secret123"));

        ArgumentCaptor<AppUserEntity> saved = ArgumentCaptor.forClass(AppUserEntity.class);
        verify(userRepository).save(saved.capture());
        assertTrue(new BCryptPasswordEncoder().matches("secret123", saved.getValue().getPasswordHash()));
    }

    @Test
    void changeMyPassword_rejectsMismatchedConfirmation() {
        var ex = assertThrows(ResponseStatusException.class, () ->
            service.changeMyPassword(USER_ID, new ChangeMyPasswordRequest("secret123", "secret124")));

        assertEquals(400, ex.getStatusCode().value());
        assertEquals("Passwords do not match", ex.getReason());
        verify(userRepository, never()).save(any());
    }

    @Test
    void changeMyPassword_rejectsTooShortPassword() {
        var ex = assertThrows(ResponseStatusException.class, () ->
            service.changeMyPassword(USER_ID, new ChangeMyPasswordRequest("abc", "abc")));

        assertEquals(400, ex.getStatusCode().value());
        verify(userRepository, never()).save(any());
    }
}
