package com.mafia.gameservice.dto;

import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Active game state for the current player.
 * myRole is only the requesting user's own role; other players' roles are hidden.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GameWithPlayersDto {

    private UUID gameId;
    private String roomCode;
    private String status;
    private String phase;
    private String myRole; // only the current user's role
    private int dayNumber;
    private List<GamePlayerDto> players;
    private String winnerTeam;
}
