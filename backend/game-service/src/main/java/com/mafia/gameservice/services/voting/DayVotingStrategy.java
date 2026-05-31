package com.mafia.gameservice.services.voting;

import com.mafia.gameservice.models.GamePlayer;
import com.mafia.gameservice.models.GameVote;
import com.mafia.gameservice.models.VoteResult;
import com.mafia.gameservice.models.VotingSession;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Strategia głosowania dziennego
 * - Wszyscy żywi gracze mogą głosować
 * - Głosy są publiczne (widoczne dla wszystkich)
 * - W przypadku remisu NIKT nie odpada
 * - Gracz z największą liczbą głosów zostaje wyeliminowany
 */
@Component
@Slf4j
public class DayVotingStrategy implements VotingStrategy {

  @Override
  public boolean validateVote(GameVote vote, GamePlayer voter) {
    // Każdy żywy gracz może głosować w dzień
    boolean isValid = voter.isAlive();

    if (!isValid) {
      log.warn(
          "Invalid day vote: voter {} is not alive",
          voter.getUser() != null ? voter.getUser().getUsername() : "unknown");
    }

    return isValid;
  }

  @Override
  public int countMafiaVotes(List<GameVote> votes, VotingSession session) {
    // Nie dotyczy dziennego głosowania - wszyscy głosują równo
    return 0;
  }

  @Override
  public VotingResult determineResult(List<VoteResult> results, VotingSession session) {
    log.info(
        "Determining day voting result for session {} with {} results",
        session.getId(),
        results.size());

    // Jeśli nikt nie głosował
    if (results.isEmpty()) {
      log.info("No votes cast - no elimination");
      return VotingResult.noElimination();
    }

    // Znajdź maksymalną liczbę głosów
    int maxVotes = results.stream().mapToInt(VoteResult::getVoteCount).max().orElse(0);

    if (maxVotes == 0) {
      log.info("All votes are zero - no elimination");
      return VotingResult.noElimination();
    }

    // Znajdź wszystkich graczy z maksymalną liczbą głosów
    List<VoteResult> topVoted =
        results.stream().filter(r -> r.getVoteCount() == maxVotes).collect(Collectors.toList());

    log.info("Top voted players: {} with {} votes each", topVoted.size(), maxVotes);

    // Jeśli jest remis (więcej niż jeden gracz z max głosami)
    if (topVoted.size() > 1) {
      log.info("Tie detected - no elimination (day voting rule)");
      return VotingResult.tie(topVoted);
    }

    // Jeden zwyci��zca - zostaje wyeliminowany
    VoteResult winner = topVoted.getFirst();
    log.info(
        "Player {} eliminated with {} votes",
        winner.getTargetUser().getUsername(),
        winner.getVoteCount());

    return VotingResult.eliminated(winner.getTargetUser());
  }

  @Override
  public boolean areVotesPublic() {
    // Głosy dzienne są publiczne - wszyscy widzą kto na kogo głosował
    return true;
  }

  @Override
  public String getStrategyName() {
    return "DAY_VOTING";
  }
}
