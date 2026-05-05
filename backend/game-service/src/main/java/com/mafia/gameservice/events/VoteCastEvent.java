package com.mafia.gameservice.events;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Zdarzenie publikowane gdy gracz oddaje głos.
 *
 * Konsumenci mogą użyć tego zdarzenia do:
 * - Analizy wzorców głosowania
 * - Wykrywania podejrzanych zachowań (np. mafia zawsze głosuje razem)
 * - Statystyk aktywności graczy
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoteCastEvent implements Serializable {

    private UUID gameId;
    private String roomCode;
    private UUID voterId;
    private String voterUsername;
    private String voterRole; // Rola głosującego
    private UUID targetId;
    private String targetUsername;
    private String votingPhase; // DAY_VOTING lub NIGHT_VOTING
    private int dayNumber;
    private int currentVoteCount; // Ile głosów już oddano w tej sesji
    private int totalEligibleVoters; // Ilu graczy może głosować
    private LocalDateTime votedAt;
}
