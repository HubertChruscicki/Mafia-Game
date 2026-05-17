package com.mafia.authservice.service;

import com.mafia.authservice.exception.TokenExpiredException;
import com.mafia.authservice.exception.TokenNotFoundException;
import com.mafia.authservice.models.RefreshToken;
import com.mafia.authservice.models.User;
import com.mafia.authservice.repository.RefreshTokenRepository;
import com.mafia.authservice.security.JwtTokenProvider;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Issues, rotates, revokes and prunes refresh tokens.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class RefreshTokenService {

    private static final int TOKEN_BYTES = 32;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;

    @Value("${jwt.refresh-expiration}")
    private long refreshExpirationMillis;

    @Value("${jwt.max-refresh-tokens-per-user}")
    private long maxTokensPerUser;

    public RefreshToken createRefreshToken(User user) {
        cleanupUserTokensIfNeeded(user);

        RefreshToken token = new RefreshToken(
                generateSecureToken(),
                user,
                LocalDateTime.now().plusNanos(refreshExpirationMillis * 1_000_000L));
        return refreshTokenRepository.save(token);
    }

    public String refreshAccessToken(String refreshToken) {
        RefreshToken stored = refreshTokenRepository.findByTokenAndRevokedFalse(refreshToken)
                .orElseThrow(() -> new TokenNotFoundException("Refresh token not found or revoked"));

        if (stored.isExpired()) {
            stored.setRevoked(true);
            refreshTokenRepository.save(stored);
            throw new TokenExpiredException("Refresh token has expired");
        }

        return jwtTokenProvider.generateToken(stored.getUser());
    }

    public RefreshToken rotateRefreshToken(String oldToken) {
        RefreshToken stored = refreshTokenRepository.findByTokenAndRevokedFalse(oldToken)
                .orElseThrow(() -> new TokenNotFoundException("Refresh token not found or revoked"));

        if (stored.isExpired()) {
            stored.setRevoked(true);
            refreshTokenRepository.save(stored);
            throw new TokenExpiredException("Refresh token has expired");
        }

        stored.setRevoked(true);
        refreshTokenRepository.save(stored);
        return createRefreshToken(stored.getUser());
    }

    public void revokeRefreshToken(String token) {
        if (token == null || token.isBlank()) {
            return;
        }
        refreshTokenRepository.revokeByToken(token);
    }

    public void revokeAllUserTokens(User user) {
        refreshTokenRepository.revokeAllByUser(user);
    }

    public void cleanupUserTokensIfNeeded(User user) {
        List<RefreshToken> active = refreshTokenRepository.findByUserAndRevokedFalse(user);
        long valid = active.stream().filter(RefreshToken::isValid).count();
        if (valid >= maxTokensPerUser) {
            // revoke oldest first
            active.stream()
                    .sorted((a, b) -> a.getCreatedAt().compareTo(b.getCreatedAt()))
                    .limit(Math.max(0, valid - maxTokensPerUser + 1))
                    .forEach(t -> {
                        t.setRevoked(true);
                        refreshTokenRepository.save(t);
                    });
        }
    }

    public String generateSecureToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * Hourly cleanup of expired and revoked tokens.
     */
    @Scheduled(fixedRate = 3_600_000L)
    public void cleanupExpiredTokens() {
        log.debug("Running scheduled refresh-token cleanup");
        refreshTokenRepository.deleteExpiredAndRevokedTokens(LocalDateTime.now());
    }
}
