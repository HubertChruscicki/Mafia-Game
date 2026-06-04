package com.mafia.authservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mafia.authservice.exception.TokenExpiredException;
import com.mafia.authservice.exception.TokenNotFoundException;
import com.mafia.authservice.models.RefreshToken;
import com.mafia.authservice.models.User;
import com.mafia.authservice.repository.RefreshTokenRepository;
import com.mafia.authservice.security.JwtTokenProvider;
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
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private JwtTokenProvider jwtTokenProvider;

    @InjectMocks private RefreshTokenService refreshTokenService;

    private User user;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(refreshTokenService, "refreshExpirationMillis", 604_800_000L);
        ReflectionTestUtils.setField(refreshTokenService, "maxTokensPerUser", 5L);
        user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername("alice");
        user.setEmail("alice@example.com");
    }

    @Test
    void createRefreshTokenStoresUniqueToken() {
        when(refreshTokenRepository.findByUserAndRevokedFalse(user)).thenReturn(List.of());
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

        RefreshToken created = refreshTokenService.createRefreshToken(user);

        assertThat(created.getToken()).isNotBlank();
        assertThat(created.getUser()).isEqualTo(user);
        assertThat(created.getExpiresAt()).isAfter(LocalDateTime.now());
    }

    @Test
    void refreshAccessTokenReturnsNewJwt() {
        RefreshToken stored = new RefreshToken("rt", user, LocalDateTime.now().plusDays(1));
        when(refreshTokenRepository.findByTokenAndRevokedFalse("rt")).thenReturn(Optional.of(stored));
        when(jwtTokenProvider.generateToken(user)).thenReturn("new-jwt");

        String result = refreshTokenService.refreshAccessToken("rt");

        assertThat(result).isEqualTo("new-jwt");
    }

    @Test
    void refreshAccessTokenRejectsUnknownToken() {
        when(refreshTokenRepository.findByTokenAndRevokedFalse("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> refreshTokenService.refreshAccessToken("missing"))
                .isInstanceOf(TokenNotFoundException.class);
    }

    @Test
    void refreshAccessTokenRejectsExpiredToken() {
        RefreshToken expired = new RefreshToken("rt", user, LocalDateTime.now().minusMinutes(1));
        when(refreshTokenRepository.findByTokenAndRevokedFalse("rt")).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> refreshTokenService.refreshAccessToken("rt"))
                .isInstanceOf(TokenExpiredException.class);
        assertThat(expired.isRevoked()).isTrue();
    }

    @Test
    void rotateRefreshTokenRevokesOldAndIssuesNew() {
        RefreshToken stored = new RefreshToken("old", user, LocalDateTime.now().plusDays(1));
        when(refreshTokenRepository.findByTokenAndRevokedFalse("old")).thenReturn(Optional.of(stored));
        when(refreshTokenRepository.findByUserAndRevokedFalse(user)).thenReturn(List.of());
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

        RefreshToken rotated = refreshTokenService.rotateRefreshToken("old");

        assertThat(stored.isRevoked()).isTrue();
        assertThat(rotated.getToken()).isNotEqualTo("old");
        assertThat(rotated.getUser()).isEqualTo(user);
    }

    @Test
    void revokeRefreshTokenIgnoresBlankInput() {
        refreshTokenService.revokeRefreshToken(" ");
        refreshTokenService.revokeRefreshToken(null);
        verify(refreshTokenRepository, never()).revokeByToken(any());
    }

    @Test
    void revokeRefreshTokenForwardsToRepository() {
        refreshTokenService.revokeRefreshToken("rt");
        verify(refreshTokenRepository).revokeByToken("rt");
    }

    @Test
    void cleanupExpiredTokensDelegatesToRepository() {
        refreshTokenService.cleanupExpiredTokens();
        verify(refreshTokenRepository).deleteExpiredAndRevokedTokens(any());
    }

    @Test
    void generateSecureTokenProducesNonEmptyValues() {
        String t1 = refreshTokenService.generateSecureToken();
        String t2 = refreshTokenService.generateSecureToken();
        assertThat(t1).isNotBlank();
        assertThat(t2).isNotBlank().isNotEqualTo(t1);
    }
}
