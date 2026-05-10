package com.mafia.authservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to change the current user's password")
public class UpdatePasswordRequest {

    @Schema(description = "Current password for verification", example = "OldSecurePassword123!", format = "password")
    @NotBlank(message = "Current password is required")
    private String oldPassword;

    @Schema(description = "New password (min 8 chars)", example = "NewSecurePassword456!", minLength = 8)
    @NotBlank(message = "New password is required")
    @Size(min = 8, message = "Password must be at least 8 characters long")
    private String newPassword;
}
