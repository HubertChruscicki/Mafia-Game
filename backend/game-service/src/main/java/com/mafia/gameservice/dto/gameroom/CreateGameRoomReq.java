package com.mafia.gameservice.dto.gameroom;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateGameRoomReq {

    @NotBlank(message = "Room name is required")
    @Size(min = 3, max = 100, message = "Room name must be between 3 and 100 characters")
    private String name;

    @Min(value = 3, message = "Minimum 3 players required")
    @Max(value = 20, message = "Maximum 20 players allowed")
    private int maxPlayers;

    public CreateGameRoomReq() {}
}
