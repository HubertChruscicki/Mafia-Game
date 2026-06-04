package com.mafia.authservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Credentials used to authenticate an existing user")
public class LoginRequest {

    @Schema(description = "User's email address", example = "john@example.com", format = "email")
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @Schema(description = "User's password", example = "SecurePassword123!", format = "password")
    @NotBlank(message = "Password is required")
    private String password;
}
