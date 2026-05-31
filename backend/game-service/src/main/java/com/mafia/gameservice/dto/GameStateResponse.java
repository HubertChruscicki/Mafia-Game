package com.mafia.gameservice.dto;

import com.mafia.gameservice.enums.GamePhase;
import com.mafia.gameservice.enums.GameStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Current game state response")
public class GameStateResponse {
  private UUID id;
  private UUID roomId;
  private GameStatus status;
  private GamePhase currentPhase;
  private int dayNumber;
  private LocalDateTime createdAt;
  private LocalDateTime startedAt;
  private LocalDateTime endedAt;
}
