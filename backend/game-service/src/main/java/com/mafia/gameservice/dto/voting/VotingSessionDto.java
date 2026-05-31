package com.mafia.gameservice.dto.voting;

import com.mafia.gameservice.enums.GamePhase;
import com.mafia.gameservice.enums.VotingStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO reprezentujący sesję głosowania
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VotingSessionDto {
  private UUID sessionId;
  private UUID gameId;
  private String roomCode;
  private GamePhase phase;
  private int dayNumber;
  private LocalDateTime startedAt;
  private LocalDateTime endsAt;
  private int totalEligibleVoters;
  private int votesReceived;
  private VotingStatus status;
  private List<VoteResultDto> currentResults;
  private Long remainingTimeSeconds;
}
