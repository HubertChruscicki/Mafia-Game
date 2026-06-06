package com.mafia.gameservice.dto.voting;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response po oddaniu głosu
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CastVoteResponse {
  private boolean success;
  private String message;
  private VotingSessionDto updatedSession;

  public static CastVoteResponse success(VotingSessionDto session) {
    return new CastVoteResponse(true, "Vote cast successfully", session);
  }

  public static CastVoteResponse error(String message) {
    return new CastVoteResponse(false, message, null);
  }
}
