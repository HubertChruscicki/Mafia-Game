package com.mafia.gameservice.dto.voting;

import com.mafia.gameservice.enums.VotingStatus;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * WebSocket indication - aktualizacja stanu głosowania
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VotingUpdateInd {
  private UUID sessionId;
  private int votesReceived;
  private int totalEligibleVoters;
  private Long remainingTimeSeconds;
  private List<VoteResultDto> currentResults;
  private VotingStatus status;
}
