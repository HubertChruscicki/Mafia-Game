package com.mafia.authservice.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mafia.authservice.dto.AuthResponse;
import com.mafia.authservice.dto.LoginRequest;
import com.mafia.authservice.dto.RefreshTokenRequest;
import com.mafia.authservice.dto.RegistrationRequest;
import com.mafia.authservice.dto.UserInfoResponse;
import com.mafia.authservice.exception.GlobalExceptionHandler;
import com.mafia.authservice.models.RefreshToken;
import com.mafia.authservice.models.User;
import com.mafia.authservice.security.JwtTokenProvider;
import com.mafia.authservice.service.RefreshTokenService;
import com.mafia.authservice.service.UserService;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Standalone MockMvc slice test for {@link AuthController}.
 */
class AuthControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private UserService userService;
    private RefreshTokenService refreshTokenService;
    private JwtTokenProvider jwtTokenProvider;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        userService = Mockito.mock(UserService.class);
        refreshTokenService = Mockito.mock(RefreshTokenService.class);
        jwtTokenProvider = Mockito.mock(JwtTokenProvider.class);
        AuthController controller = new AuthController(userService, refreshTokenService, jwtTokenProvider);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void registerReturnsCreated() throws Exception {
        RegistrationRequest request = new RegistrationRequest("bob", "bob@example.com", "Secret123!");
        UserInfoResponse response = UserInfoResponse.builder()
                .id(UUID.randomUUID())
                .username("bob")
                .email("bob@example.com")
                .admin(false)
                .build();
        when(userService.registerUser(any())).thenReturn(response);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("bob"))
                .andExpect(jsonPath("$.email").value("bob@example.com"));
    }

    @Test
    void registerRejectsInvalidEmail() throws Exception {
        RegistrationRequest request = new RegistrationRequest("bob", "not-an-email", "Secret123!");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void loginReturnsTokens() throws Exception {
        LoginRequest request = new LoginRequest("alice@example.com", "Secret123!");
        AuthResponse response = AuthResponse.builder()
                .token("jwt")
                .refreshToken("refresh")
                .expiresIn(3600L)
                .build();
        when(userService.authenticateUser(any())).thenReturn(response);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt"))
                .andExpect(jsonPath("$.refreshToken").value("refresh"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    @Test
    void loginReturnsUnauthorizedOnBadCredentials() throws Exception {
        LoginRequest request = new LoginRequest("alice@example.com", "Wrong!");
        when(userService.authenticateUser(any())).thenThrow(new BadCredentialsException("nope"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refreshReturnsRotatedTokens() throws Exception {
        RefreshTokenRequest request = new RefreshTokenRequest("old-refresh");
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername("alice");
        user.setEmail("alice@example.com");

        RefreshToken rotated = new RefreshToken("new-refresh", user, LocalDateTime.now().plusDays(1));
        when(refreshTokenService.rotateRefreshToken("old-refresh")).thenReturn(rotated);
        when(jwtTokenProvider.generateToken(user)).thenReturn("new-jwt");
        when(jwtTokenProvider.getExpirationSeconds()).thenReturn(3600L);

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("new-jwt"))
                .andExpect(jsonPath("$.refreshToken").value("new-refresh"));
    }

    @Test
    void logoutReturnsNoContent() throws Exception {
        RefreshTokenRequest request = new RefreshTokenRequest("refresh");

        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        verify(userService).logoutUser("refresh");
    }
}
