package com.mafia.authservice.security;

import com.mafia.authservice.models.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

/**
 * Issues and validates JWT access tokens used across the platform.
 */
@Component
@Slf4j
public class JwtTokenProvider {

    private static final String CLAIM_USERNAME = "username";
    private static final String CLAIM_EMAIL = "email";
    private static final String CLAIM_IS_ADMIN = "isAdmin";

    private final SecretKey signingKey;
    private final long expirationMillis;

    public JwtTokenProvider(@Value("${jwt.secret}") String secret,
                            @Value("${jwt.expiration}") long expirationMillis) {
        byte[] keyBytes = Base64.getDecoder().decode(secret);
        this.signingKey = new SecretKeySpec(keyBytes, "HmacSHA512");
        this.expirationMillis = expirationMillis;
    }

    public String generateToken(User user) {
        Date now = new Date();
        Date expiresAt = new Date(now.getTime() + expirationMillis);

        return Jwts.builder()
                .subject(user.getId().toString())
                .claim(CLAIM_USERNAME, user.getUsername())
                .claim(CLAIM_EMAIL, user.getEmail())
                .claim(CLAIM_IS_ADMIN, user.isAdmin())
                .issuedAt(now)
                .expiration(expiresAt)
                .signWith(signingKey, Jwts.SIG.HS512)
                .compact();
    }

    public UUID getUserIdFromToken(String token) {
        return UUID.fromString(parse(token).getSubject());
    }

    public Authentication getAuthentication(String token) {
        Claims claims = parse(token);
        UUID userId = UUID.fromString(claims.getSubject());
        String username = claims.get(CLAIM_USERNAME, String.class);
        String email = claims.get(CLAIM_EMAIL, String.class);
        Boolean isAdmin = claims.get(CLAIM_IS_ADMIN, Boolean.class);
        boolean admin = isAdmin != null && isAdmin;

        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        if (admin) {
            authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        }

        User principal = new User();
        principal.setId(userId);
        principal.setUsername(username);
        principal.setEmail(email);
        principal.setAdmin(admin);

        return new UsernamePasswordAuthenticationToken(principal, "", authorities);
    }

    public boolean validateToken(String token) {
        try {
            parse(token);
            return true;
        } catch (Exception ex) {
            log.debug("Invalid JWT token: {}", ex.getMessage());
            return false;
        }
    }

    public long getExpirationSeconds() {
        return expirationMillis / 1000;
    }

    private Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
