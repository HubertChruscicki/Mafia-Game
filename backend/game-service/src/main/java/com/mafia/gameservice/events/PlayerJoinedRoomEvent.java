package com.mafia.gameservice.events;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Zdarzenie publikowane gdy gracz dołącza do pokoju.
 *
 * Konsumenci mogą użyć tego zdarzenia do:
 * - Logowania aktywności w pokojach
 * - Śledzenia popularności pokojów
 * - Powiadomień dla hosta o nowym graczu
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlayerJoinedRoomEvent implements Serializable {

    private UUID roomId;
    private String roomCode;
    private String roomName;
    private UUID playerId;
    private String playerUsername;
    private int currentPlayerCount;
    private int maxPlayers;
    private LocalDateTime joinedAt;
}
