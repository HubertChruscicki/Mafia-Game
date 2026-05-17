package com.mafia.authservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mafia.authservice.dto.UserResponse;
import com.mafia.authservice.exception.UserNotFoundException;
import com.mafia.authservice.models.User;
import com.mafia.authservice.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private RefreshTokenService refreshTokenService;

    @InjectMocks private AdminService adminService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername("alice");
        user.setEmail("alice@example.com");
        user.setPasswordHash("hash");
        user.setAdmin(false);
    }

    @Test
    void getAllUsersMapsToDto() {
        when(userRepository.findAll()).thenReturn(List.of(user));

        List<UserResponse> result = adminService.getAllUsers();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUsername()).isEqualTo("alice");
    }

    @Test
    void updateUserAdminFlagPromotesAndRevokesTokens() {
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        UserResponse response = adminService.updateUserAdminFlag(user.getId(), true);

        assertThat(response.isAdmin()).isTrue();
        assertThat(user.isAdmin()).isTrue();
        verify(refreshTokenService).revokeAllUserTokens(user);
    }

    @Test
    void updateUserAdminFlagThrowsWhenMissing() {
        UUID missing = UUID.randomUUID();
        when(userRepository.findById(missing)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminService.updateUserAdminFlag(missing, true))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void deleteUserRevokesTokensAndDeletes() {
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        adminService.deleteUser(user.getId());

        verify(refreshTokenService).revokeAllUserTokens(user);
        verify(userRepository).delete(user);
    }

    @Test
    void deleteUserThrowsWhenMissing() {
        UUID missing = UUID.randomUUID();
        when(userRepository.findById(missing)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminService.deleteUser(missing))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void allUsersPreservesCreationTimestamp() {
        user.setCreatedAt(LocalDateTime.of(2026, 5, 1, 12, 0));
        when(userRepository.findAll()).thenReturn(List.of(user));

        UserResponse mapped = adminService.getAllUsers().get(0);

        assertThat(mapped.getCreatedAt()).isEqualTo(LocalDateTime.of(2026, 5, 1, 12, 0));
    }
}
