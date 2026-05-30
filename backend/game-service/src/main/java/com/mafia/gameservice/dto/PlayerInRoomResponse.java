package com.mafia.gameservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Schema(description = "Player information within a game room")
public class PlayerInRoomResponse {

  @Schema(
      description = "Player's unique identifier",
      example = "456e7890-e89b-12d3-a456-426614174001")
  private UUID playerId;

  @Schema(
      description = "User's unique identifier",
      example = "456e7890-e89b-12d3-a456-426614174001")
  private UUID userId;

  @Schema(description = "Player's username", example = "john_doe")
  private String username;

  @Schema(description = "Whether this player is the room host", example = "true")
  private boolean isHost;

  @Schema(description = "Whether the player is ready to start the game", example = "false")
  private boolean isReady;

  @Schema(description = "When player joined the room", example = "2024-01-15T10:30:00")
  private LocalDateTime joinedAt;

  public PlayerInRoomResponse() {}

  public PlayerInRoomResponse(UUID userId, String username, boolean isHost, boolean isReady) {
    this.playerId = UUID.randomUUID();
    this.userId = playerId;
    this.username = username;
    this.isHost = isHost;
    this.isReady = isReady;
    this.joinedAt = LocalDateTime.now();
  }

  public PlayerInRoomResponse(
      UUID playerId, UUID userId, String username, boolean isHost, LocalDateTime joinedAt) {
    this.playerId = playerId;
    this.userId = userId;
    this.username = username;
    this.isHost = isHost;
    this.isReady = false;
    this.joinedAt = joinedAt;
  }
}
