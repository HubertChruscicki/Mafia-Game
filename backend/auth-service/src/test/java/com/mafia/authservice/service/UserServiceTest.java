package com.mafia.authservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mafia.authservice.dto.AuthResponse;
import com.mafia.authservice.dto.LoginRequest;
import com.mafia.authservice.dto.RegistrationRequest;
import com.mafia.authservice.dto.UserInfoResponse;
import com.mafia.authservice.exception.EmailAlreadyExistsException;
import com.mafia.authservice.exception.InvalidPasswordException;
import com.mafia.authservice.exception.UsernameAlreadyExistsException;
import com.mafia.authservice.models.RefreshToken;
import com.mafia.authservice.models.User;
import com.mafia.authservice.repository.UserRepository;
import com.mafia.authservice.security.JwtTokenProvider;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private RefreshTokenService refreshTokenService;

    @InjectMocks private UserService userService;

    private User existingUser;

    @BeforeEach
    void setUp() {
        existingUser = new User();
        existingUser.setId(UUID.randomUUID());
        existingUser.setUsername("alice");
        existingUser.setEmail("alice@example.com");
        existingUser.setPasswordHash("hashed");
        existingUser.setAdmin(false);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void registerUserPersistsHashedPassword() {
        RegistrationRequest request = new RegistrationRequest("bob", "bob@example.com", "Secret123!");
        when(userRepository.existsByUsername("bob")).thenReturn(false);
        when(userRepository.existsByEmail("bob@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Secret123!")).thenReturn("hashed-pwd");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(UUID.randomUUID());
            return u;
        });

        UserInfoResponse response = userService.registerUser(request);

        assertThat(response.getUsername()).isEqualTo("bob");
        assertThat(response.getEmail()).isEqualTo("bob@example.com");
        assertThat(response.isAdmin()).isFalse();
        verify(passwordEncoder).encode("Secret123!");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void registerUserRejectsDuplicateUsername() {
        RegistrationRequest request = new RegistrationRequest("bob", "bob@example.com", "Secret123!");
        when(userRepository.existsByUsername("bob")).thenReturn(true);

        assertThatThrownBy(() -> userService.registerUser(request))
                .isInstanceOf(UsernameAlreadyExistsException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void registerUserRejectsDuplicateEmail() {
        RegistrationRequest request = new RegistrationRequest("bob", "bob@example.com", "Secret123!");
        when(userRepository.existsByUsername("bob")).thenReturn(false);
        when(userRepository.existsByEmail("bob@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.registerUser(request))
                .isInstanceOf(EmailAlreadyExistsException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void authenticateUserReturnsTokens() {
        LoginRequest login = new LoginRequest("alice@example.com", "Secret123!");
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches("Secret123!", "hashed")).thenReturn(true);
        when(jwtTokenProvider.generateToken(existingUser)).thenReturn("jwt-token");
        when(jwtTokenProvider.getExpirationSeconds()).thenReturn(3600L);

        RefreshToken refreshToken = new RefreshToken("refresh-xyz", existingUser, LocalDateTime.now().plusDays(1));
        when(refreshTokenService.createRefreshToken(existingUser)).thenReturn(refreshToken);

        AuthResponse response = userService.authenticateUser(login);

        assertThat(response.getToken()).isEqualTo("jwt-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-xyz");
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getExpiresIn()).isEqualTo(3600L);
    }

    @Test
    void authenticateUserRejectsBadPassword() {
        LoginRequest login = new LoginRequest("alice@example.com", "Wrong!");
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches("Wrong!", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> userService.authenticateUser(login))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void authenticateUserRejectsUnknownEmail() {
        LoginRequest login = new LoginRequest("ghost@example.com", "Secret123!");
        when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.authenticateUser(login))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void updatePasswordRequiresMatchingOldPassword() {
        authenticate(existingUser);
        when(userRepository.findById(existingUser.getId())).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches("Wrong!", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> userService.updatePassword("Wrong!", "NewPwd123!"))
                .isInstanceOf(InvalidPasswordException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void updatePasswordRevokesAllTokensOnSuccess() {
        authenticate(existingUser);
        when(userRepository.findById(existingUser.getId())).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches("Secret123!", "hashed")).thenReturn(true);
        when(passwordEncoder.encode("NewPwd123!")).thenReturn("new-hash");

        userService.updatePassword("Secret123!", "NewPwd123!");

        verify(userRepository).save(existingUser);
        verify(refreshTokenService, times(1)).revokeAllUserTokens(existingUser);
        assertThat(existingUser.getPasswordHash()).isEqualTo("new-hash");
    }

    @Test
    void updateUsernameRejectsTakenName() {
        authenticate(existingUser);
        when(userRepository.findById(existingUser.getId())).thenReturn(Optional.of(existingUser));
        when(userRepository.existsByUsername("taken")).thenReturn(true);

        assertThatThrownBy(() -> userService.updateUsername("taken"))
                .isInstanceOf(UsernameAlreadyExistsException.class);
    }

    private void authenticate(User user) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, "", java.util.List.of()));
    }
}
