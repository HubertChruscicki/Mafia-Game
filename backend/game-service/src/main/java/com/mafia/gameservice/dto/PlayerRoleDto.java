package com.mafia.gameservice.dto;

import com.mafia.gameservice.enums.GameRole;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * DTO zawierające informacje o roli gracza w grze
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlayerRoleDto {
    private UUID userId;
    private String username;
    private GameRole role;
    private boolean isAlive;
    private String gameNick;
}
