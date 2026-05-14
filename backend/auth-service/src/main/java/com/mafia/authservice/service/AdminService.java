package com.mafia.authservice.service;

import com.mafia.authservice.dto.UserResponse;
import com.mafia.authservice.exception.UserNotFoundException;
import com.mafia.authservice.models.User;
import com.mafia.authservice.repository.UserRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Administrative operations restricted to users with the ADMIN role.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AdminService {

    private final UserRepository userRepository;
    private final RefreshTokenService refreshTokenService;

    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream().map(this::toUserResponse).toList();
    }

    public UserResponse updateUserAdminFlag(UUID userId, boolean admin) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId.toString()));
        user.setAdmin(admin);
        User saved = userRepository.save(user);
        // Force re-login on role change so the new authorities take effect.
        refreshTokenService.revokeAllUserTokens(saved);
        log.info("Admin flag for user {} set to {}", saved.getId(), admin);
        return toUserResponse(saved);
    }

    public void deleteUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId.toString()));
        refreshTokenService.revokeAllUserTokens(user);
        userRepository.delete(user);
        log.info("Deleted user {}", userId);
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
