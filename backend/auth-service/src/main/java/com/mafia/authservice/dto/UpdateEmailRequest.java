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
@Schema(description = "Request to update the current user's email address")
public class UpdateEmailRequest {

    @Schema(description = "New email address", example = "newemail@example.com", format = "email")
    @NotBlank(message = "New email is required")
    @Email(message = "Invalid email format")
    private String newEmail;
}
