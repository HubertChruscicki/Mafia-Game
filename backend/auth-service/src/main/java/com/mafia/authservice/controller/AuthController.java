package com.mafia.authservice.controller;

import com.mafia.authservice.dto.AuthResponse;
import com.mafia.authservice.dto.LoginRequest;
import com.mafia.authservice.dto.RefreshTokenRequest;
import com.mafia.authservice.dto.RegistrationRequest;
import com.mafia.authservice.dto.UserInfoResponse;
import com.mafia.authservice.models.RefreshToken;
import com.mafia.authservice.security.JwtTokenProvider;
import com.mafia.authservice.service.RefreshTokenService;
import com.mafia.authservice.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Register, login, refresh and logout")
public class AuthController {

    private final UserService userService;
    private final RefreshTokenService refreshTokenService;
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping("/register")
    @Operation(summary = "Register a new user account")
    public ResponseEntity<UserInfoResponse> register(@Valid @RequestBody RegistrationRequest request) {
        UserInfoResponse created = userService.registerUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate an existing user and obtain JWT tokens")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(userService.authenticateUser(request));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Exchange a refresh token for a new access token (rotates refresh token)")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        RefreshToken rotated = refreshTokenService.rotateRefreshToken(request.getRefreshToken());
        String newAccess = jwtTokenProvider.generateToken(rotated.getUser());
        AuthResponse response = AuthResponse.builder()
                .token(newAccess)
                .refreshToken(rotated.getToken())
                .expiresIn(jwtTokenProvider.getExpirationSeconds())
                .build();
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    @Operation(summary = "Revoke the supplied refresh token")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshTokenRequest request) {
        userService.logoutUser(request.getRefreshToken());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/logout-all")
    @Operation(summary = "Revoke every refresh token for the current user")
    public ResponseEntity<Void> logoutAll() {
        userService.logoutAllDevices();
        return ResponseEntity.noContent().build();
    }
}
