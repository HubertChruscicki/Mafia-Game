package com.mafia.gameservice.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.mafia.gameservice.models.User;
import com.mafia.gameservice.support.GameTestFixtures;
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
    void generateAndValidateToken() {
        User user = GameTestFixtures.user("alice");
        user.setAdmin(true);

        String token = provider.generateToken(user);

        assertThat(provider.validateToken(token)).isTrue();
        assertThat(provider.getUserIdFromToken(token)).isEqualTo(user.getId());

        Authentication auth = provider.getAuthentication(token);
        User principal = (User) auth.getPrincipal();
        assertThat(principal.getUsername()).isEqualTo("alice");
        assertThat(auth.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN");
    }

    @Test
    void validateTokenRejectsInvalidInput() {
        assertThat(provider.validateToken("bad-token")).isFalse();
    }

    @Test
    void getExpirationSeconds() {
        assertThat(provider.getExpirationSeconds()).isEqualTo(3600L);
    }
}
