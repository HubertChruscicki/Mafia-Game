package com.mafia.gameservice.events;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Zdarzenie publikowane gdy następuje zmiana fazy gry.
 *
 * Fazy gry: LOBBY -> NIGHT_VOTING -> NIGHT_RESULT -> DAY_VOTING -> DAY_RESULT -> ...
 *
 * Konsumenci mogą użyć tego zdarzenia do:
 * - Logowania przebiegu gry
 * - Śledzenia czasu trwania poszczególnych faz
 * - Analityki zachowań graczy w różnych fazach
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GamePhaseChangedEvent implements Serializable {

    private UUID gameId;
    private String roomCode;
    private String previousPhase;
    private String newPhase;
    private int dayNumber;
    private int alivePlayersCount;
    private int aliveMafiaCount;
    private int aliveCitizensCount;
    private LocalDateTime changedAt;
}
