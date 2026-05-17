package com.mafia.authservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to toggle a user's admin privileges")
public class UpdateAdminFlagRequest {

    @Schema(description = "Whether the user should be an admin", example = "true")
    private boolean admin;
}
