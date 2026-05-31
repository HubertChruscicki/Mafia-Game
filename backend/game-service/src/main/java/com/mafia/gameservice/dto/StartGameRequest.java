package com.mafia.gameservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to start a new game in a room")
public class StartGameRequest {
  @Schema(
      description = "Room identifier where to start the game",
      requiredMode = Schema.RequiredMode.REQUIRED)
  @NotNull
  private UUID roomId;

  @Schema(description = "Number of mafia players", example = "1", defaultValue = "1")
  @Min(value = 1, message = "At least 1 mafia player is required")
  @Max(value = 10, message = "Maximum 10 mafia players allowed")
  private int mafiaCount = 1;

  @Schema(
      description = "Discussion time in seconds for voting phases",
      example = "120",
      defaultValue = "120")
  @Min(value = 30, message = "Minimum discussion time is 30 seconds")
  @Max(value = 600, message = "Maximum discussion time is 600 seconds (10 minutes)")
  private int discussionTimeSeconds = 120;
}
