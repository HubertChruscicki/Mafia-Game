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
 * Zdarzenie publikowane gdy gra zostaje rozpoczęta.
 *
 * Konsumenci mogą użyć tego zdarzenia do:
 * - Logowania rozpoczęcia gry
 * - Aktualizacji statystyk (liczba rozpoczętych gier)
 * - Wysyłania powiadomień do zewnętrznych systemów
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GameStartedEvent implements Serializable {

    private UUID gameId;
    private UUID roomId;
    private String roomCode;
    private String roomName;
    private int playerCount;
    private int mafiaCount;
    private int citizenCount;
    private List<PlayerInfo> players;
    private LocalDateTime startedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PlayerInfo implements Serializable {
        private UUID userId;
        private String username;
        private String role; // MAFIA lub CITIZEN
    }
}
