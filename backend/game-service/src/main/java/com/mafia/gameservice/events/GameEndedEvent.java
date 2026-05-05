package com.mafia.gameservice.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Zdarzenie publikowane gdy gra zostaje zakończona.
 *
 * Konsumenci mogą użyć tego zdarzenia do:
 * - Aktualizacji statystyk graczy (wygrane/przegrane)
 * - Aktualizacji rankingu
 * - Generowania raportów z gry
 * - Wysyłania powiadomień o zakończeniu
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GameEndedEvent implements Serializable {

    private UUID gameId;
    private String roomCode;
    private String roomName;
    private String winner; // "MAFIA" lub "CITIZENS"
    private String winReason;
    private int totalDays;
    private int durationSeconds;
    private List<PlayerResult> playerResults;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PlayerResult implements Serializable {
        private UUID userId;
        private String username;
        private String role;
        private boolean survived;
        private int eliminatedOnDay; // 0 jeśli przeżył
        private int votesCast;
        private int votesReceived;
        private boolean isWinner;
    }
}
