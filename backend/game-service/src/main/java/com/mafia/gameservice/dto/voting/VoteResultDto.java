package com.mafia.gameservice.dto.voting;

import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO reprezentujący zagregowane wyniki głosowania dla jednego gracza
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VoteResultDto {
  private UUID targetUserId;
  private String targetUsername;
  private int voteCount;
  private int mafiaVoteCount; // tylko dla NIGHT_VOTE
  private List<VoterDto> voters; // lista głosujących (tylko dla DAY_VOTE)
}
