package com.mafia.gameservice.events;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Zdarzenie publikowane gdy gracz zostaje wyeliminowany.
 *
 * Konsumenci mogą użyć tego zdarzenia do:
 * - Aktualizacji statystyk gracza (liczba śmierci, w jakiej fazie)
 * - Analizy które role są eliminowane najczęściej
 * - Wysyłania powiadomień
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlayerEliminatedEvent implements Serializable {

    private UUID gameId;
    private String roomCode;
    private UUID eliminatedUserId;
    private String eliminatedUsername;
    private String eliminatedRole; // MAFIA lub CITIZEN
    private String eliminationPhase; // DAY_VOTING lub NIGHT_VOTING
    private int dayNumber;
    private int votesReceived;
    private int remainingPlayers;
    private int remainingMafia;
    private int remainingCitizens;
    private LocalDateTime eliminatedAt;
}
