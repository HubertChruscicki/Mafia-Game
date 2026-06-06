package com.mafia.gameservice.dto.voting;

import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * WebSocket indication - zakończenie głosowania
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VotingCompleteInd {
  private UUID sessionId;
  private UUID eliminatedUserId;
  private String eliminatedUsername;
  private boolean isTie;
  private String resultType; // ELIMINATION, TIE_NO_ELIMINATION, etc.
  private List<VoteResultDto> finalResults;
}
