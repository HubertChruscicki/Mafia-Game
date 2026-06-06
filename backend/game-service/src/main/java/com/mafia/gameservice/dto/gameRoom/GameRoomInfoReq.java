package com.mafia.gameservice.dto.gameRoom;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to fetch game room information - provide either roomCode or userId")
public class GameRoomInfoReq {

  @Schema(
      description = "Room code to fetch specific room (e.g., 'ABC123')",
      example = "ABC123",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("roomCode")
  private String roomCode;

  @Schema(
      description = "User ID to fetch all rooms where user is a member",
      example = "550e8400-e29b-41d4-a716-446655440000",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("userId")
  private UUID userId;

  /**
   * Validates that at least one filter is provided
   * @return true if either roomCode or userId is provided
   */
  public boolean isValid() {
    return (roomCode != null && !roomCode.isBlank()) || userId != null;
  }

  /**
   * Checks if this request is for a specific room
   * @return true if roomCode is provided
   */
  public boolean isRoomCodeSearch() {
    return roomCode != null && !roomCode.isBlank();
  }

  /**
   * Checks if this request is for user's rooms
   * @return true if userId is provided
   */
  public boolean isUserIdSearch() {
    return userId != null;
  }
}
