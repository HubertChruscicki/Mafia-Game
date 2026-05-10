package com.mafia.authservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Refresh token exchange payload")
public class RefreshTokenRequest {

    @Schema(description = "Refresh token previously issued by the service", example = "abc123...")
    @NotBlank(message = "Refresh token is required")
    private String refreshToken;
}
