package com.mafia.gameservice.dto.gameRoom;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class CreateGameRoomReq {

  @NotBlank(message = "Room name is required")
  @Size(max = 50, message = "Room name cannot exceed 50 characters")
  private String name;

  @NotNull(message = "Max players is required")
  @Min(value = 3, message = "Minimum 3 players required")
  @Max(value = 20, message = "Maximum 20 players allowed")
  private Integer maxPlayers;

  public CreateGameRoomReq() {}
}
