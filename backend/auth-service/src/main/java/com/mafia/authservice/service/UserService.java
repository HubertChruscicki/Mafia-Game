package com.mafia.authservice.service;

import com.mafia.authservice.dto.AuthResponse;
import com.mafia.authservice.dto.LoginRequest;
import com.mafia.authservice.dto.RegistrationRequest;
import com.mafia.authservice.dto.UserInfoResponse;
import com.mafia.authservice.dto.UserResponse;
import com.mafia.authservice.exception.EmailAlreadyExistsException;
import com.mafia.authservice.exception.InvalidPasswordException;
import com.mafia.authservice.exception.UserNotFoundException;
import com.mafia.authservice.exception.UsernameAlreadyExistsException;
import com.mafia.authservice.models.RefreshToken;
import com.mafia.authservice.models.User;
import com.mafia.authservice.repository.UserRepository;
import com.mafia.authservice.security.JwtTokenProvider;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Domain operations for user accounts (registration, login, profile management).
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;

    public UserInfoResponse registerUser(RegistrationRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new UsernameAlreadyExistsException(request.getUsername());
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException(request.getEmail());
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setAdmin(false);

        User saved = userRepository.save(user);
        log.info("Registered user {} ({})", saved.getUsername(), saved.getId());
        return toUserInfo(saved);
    }

    public AuthResponse authenticateUser(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        String accessToken = jwtTokenProvider.generateToken(user);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        return AuthResponse.builder()
                .token(accessToken)
                .refreshToken(refreshToken.getToken())
                .expiresIn(jwtTokenProvider.getExpirationSeconds())
                .build();
    }

    public void logoutUser(String refreshToken) {
        refreshTokenService.revokeRefreshToken(refreshToken);
    }

    public void logoutAllDevices() {
        User user = getCurrentAuthenticatedUser();
        refreshTokenService.revokeAllUserTokens(user);
    }

    @Transactional(readOnly = true)
    public UserInfoResponse getCurrentUserInfo() {
        return toUserInfo(getCurrentAuthenticatedUser());
    }

    @Transactional(readOnly = true)
    public UserResponse getCurrentUserProfile() {
        return toUserResponse(getCurrentAuthenticatedUser());
    }

    @Transactional(readOnly = true)
    public List<UserResponse> searchUsers(String query) {
        if (query == null || query.isBlank()) {
            return userRepository.findAll().stream().map(this::toUserResponse).toList();
        }
        return userRepository.findByUsernameContainingIgnoreCase(query.trim())
                .stream()
                .map(this::toUserResponse)
                .toList();
    }

    public UserInfoResponse updateUsername(String newUsername) {
        User user = getCurrentAuthenticatedUser();
        if (!user.getUsername().equals(newUsername)
                && userRepository.existsByUsername(newUsername)) {
            throw new UsernameAlreadyExistsException(newUsername);
        }
        user.setUsername(newUsername);
        return toUserInfo(userRepository.save(user));
    }

    public UserInfoResponse updateEmail(String newEmail) {
        User user = getCurrentAuthenticatedUser();
        if (!user.getEmail().equals(newEmail)
                && userRepository.existsByEmail(newEmail)) {
            throw new EmailAlreadyExistsException(newEmail);
        }
        user.setEmail(newEmail);
        return toUserInfo(userRepository.save(user));
    }

    public void updatePassword(String oldPassword, String newPassword) {
        User user = getCurrentAuthenticatedUser();
        if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
            throw new InvalidPasswordException("Current password is incorrect");
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        refreshTokenService.revokeAllUserTokens(user);
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(UUID id) {
        return userRepository.findById(id)
                .map(this::toUserResponse)
                .orElseThrow(() -> new UserNotFoundException(id.toString()));
    }

    @Transactional(readOnly = true)
    public User getCurrentAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof User principal)) {
            throw new UserNotFoundException("anonymous");
        }
        return userRepository.findById(principal.getId())
                .orElseThrow(() -> new UserNotFoundException(principal.getId().toString()));
    }

    private UserInfoResponse toUserInfo(User user) {
        return UserInfoResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .admin(user.isAdmin())
                .build();
    }

    private UserResponse toUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .admin(user.isAdmin())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
