package com.mafia.authservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Detailed user information including creation timestamp")
public class UserResponse {

    @Schema(description = "User identifier")
    private UUID id;

    @Schema(description = "Username", example = "john_doe")
    private String username;

    @Schema(description = "Email address", example = "john@example.com")
    private String email;

    @Schema(description = "Whether the user has admin privileges", example = "false")
    private boolean admin;

    @Schema(description = "Account creation timestamp")
    private LocalDateTime createdAt;
}
