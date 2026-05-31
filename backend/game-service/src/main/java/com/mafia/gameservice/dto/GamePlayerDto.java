package com.mafia.gameservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.mafia.gameservice.enums.GameRole;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO dla gracza w grze
 * Zawiera informacje o graczu, jego roli (jeśli widoczna) i statusie
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GamePlayerDto {
  private UUID userId;
  private String username;
  private String gameNick;
  private GameRole assignedRole; // null jeśli nie jest właścicielem lub gra nie zakończona
  
  @JsonProperty("isAlive")
  private boolean isAlive;
  
  @JsonProperty("isCurrentUser")
  private boolean isCurrentUser;
}
