package com.mafia.gameservice.dto;

import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GameStateResponse {

    private UUID gameId;
    private String roomCode;
    private String status;
    private String phase;
    private int dayNumber;
    private List<GamePlayerDto> players;
}
