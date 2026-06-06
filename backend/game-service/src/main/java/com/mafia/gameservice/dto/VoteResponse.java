package com.mafia.gameservice.dto;

import com.mafia.gameservice.enums.GamePhase;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class VoteResponse {

    private String status;
    private UUID gameId;
    private GamePhase currentPhase;
    private Integer dayNumber;
    private Long votesReceived;
    private Long totalPlayersAlive;
    private Boolean votingComplete;
    private UUID eliminatedPlayerId;
    private String eliminatedPlayerUsername;
    private String message;
}
