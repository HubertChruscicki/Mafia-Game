package com.mafia.gameservice.dto.voting;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * WebSocket indication - aktualizacja timera
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VotingTimerUpdateInd {
  private UUID sessionId;
  private long remainingSeconds;
}
