package com.mafia.authservice.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.mafia.authservice.models.User;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

class JwtTokenProviderTest {

    private static final String SECRET =
            "VGVzdFNlY3JldEZvclVuaXRUZXN0c09ubHlOb3RGb3JQcm9kdWN0aW9uVXNlVGVzdFNlY3JldEZvclVuaXRUZXN0c09ubHlOb3RGb3JQcm9kdWN0aW9uVXNlMTIzNDU2Nzg=";

    private JwtTokenProvider provider;

    @BeforeEach
    void setUp() {
        provider = new JwtTokenProvider(SECRET, 3_600_000L);
    }

    @Test
    void generateTokenContainsExpectedClaims() {
        User user = sampleUser(false);

        String token = provider.generateToken(user);
        assertThat(token).isNotBlank();
        assertThat(provider.validateToken(token)).isTrue();

        Authentication authentication = provider.getAuthentication(token);
        User principal = (User) authentication.getPrincipal();
        assertThat(principal.getId()).isEqualTo(user.getId());
        assertThat(principal.getUsername()).isEqualTo(user.getUsername());
        assertThat(principal.getEmail()).isEqualTo(user.getEmail());
        assertThat(authentication.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_USER");
    }

    @Test
    void getAuthenticationGrantsAdminRoleWhenFlagSet() {
        User user = sampleUser(true);

        String token = provider.generateToken(user);
        Authentication authentication = provider.getAuthentication(token);

        assertThat(authentication.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN");
    }

    @Test
    void validateTokenReturnsFalseForGarbage() {
        assertThat(provider.validateToken("not-a-jwt")).isFalse();
        assertThat(provider.validateToken("")).isFalse();
    }

    @Test
    void validateTokenReturnsFalseForExpiredToken() {
        JwtTokenProvider shortLived = new JwtTokenProvider(SECRET, 1L);
        User user = sampleUser(false);
        String token = shortLived.generateToken(user);
        try {
            Thread.sleep(10L);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
        assertThat(shortLived.validateToken(token)).isFalse();
    }

    @Test
    void getExpirationSecondsConvertsMillisToSeconds() {
        assertThat(provider.getExpirationSeconds()).isEqualTo(3600L);
    }

    @Test
    void getUserIdFromTokenReturnsSubject() {
        User user = sampleUser(false);
        String token = provider.generateToken(user);
        assertThat(provider.getUserIdFromToken(token)).isEqualTo(user.getId());
    }

    private User sampleUser(boolean admin) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername("alice");
        user.setEmail("alice@example.com");
        user.setPasswordHash("ignored");
        user.setAdmin(admin);
        return user;
    }
}
