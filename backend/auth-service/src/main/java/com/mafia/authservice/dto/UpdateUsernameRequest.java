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
@Schema(description = "Request to update the current user's username")
public class UpdateUsernameRequest {

    @Schema(description = "New username (3-20 chars)", example = "new_cool_username", minLength = 3, maxLength = 20)
    @NotBlank(message = "New username is required")
    @Size(min = 3, max = 20, message = "Username must be between 3 and 20 characters")
    private String newUsername;
}
