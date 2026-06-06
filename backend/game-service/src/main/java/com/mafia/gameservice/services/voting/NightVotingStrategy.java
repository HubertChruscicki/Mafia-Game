package com.mafia.gameservice.services.voting;

import com.mafia.gameservice.models.GamePlayer;
import com.mafia.gameservice.models.GameVote;
import com.mafia.gameservice.models.VoteResult;
import com.mafia.gameservice.models.VotingSession;
import com.mafia.gameservice.enums.GameRole;
import com.mafia.gameservice.repositories.GamePlayerRepository;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Strategia głosowania nocnego (mafia)
 * - Wszyscy żywi gracze mogą głosować, ale liczą się TYLKO głosy mafii
 * - Głosy są tajne (nie widać kto na kogo głosował)
 * - W przypadku remisu wybierany jest LOSOWO jeden z graczy z największą liczbą głosów mafii
 * - Gracz z największą liczbą głosów mafii zostaje wyeliminowany
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NightVotingStrategy implements VotingStrategy {

  private final GamePlayerRepository gamePlayerRepository;

  @Override
  public boolean validateVote(GameVote vote, GamePlayer voter) {
    // Każdy żywy gracz może głosować w nocy
    // (ale tylko głosy mafii będą liczone w wynikach)
    boolean isValid = voter.isAlive();

    if (!isValid) {
      log.warn(
          "Invalid night vote: voter {} is not alive",
          voter.getUser() != null ? voter.getUser().getUsername() : "unknown");
    }

    return isValid;
  }

  @Override
  public int countMafiaVotes(List<GameVote> votes, VotingSession session) {
    // Policz ile głosów pochodzi od mafii
    int mafiaVoteCount =
        (int)
            votes.stream()
                .filter(
                    vote -> {
                      GamePlayer voter =
                          gamePlayerRepository
                              .findByGameAndUser_Id(vote.getGame(), vote.getVoterId())
                              .orElse(null);

                      boolean isMafia =
                          voter != null && voter.getAssignedRole() == GameRole.MAFIA;

                      if (isMafia) {
                        log.debug(
                            "Mafia vote counted from player {}",
                            voter.getUser() != null ? voter.getUser().getUsername() : "unknown");
                      }

                      return isMafia;
                    })
                .count();

    log.debug("Total mafia votes counted: {}", mafiaVoteCount);
    return mafiaVoteCount;
  }

  @Override
  public VotingResult determineResult(List<VoteResult> results, VotingSession session) {
    log.info(
        "Determining night voting result for session {} with {} results",
        session.getId(),
        results.size());

    // Jeśli nikt nie głosował
    if (results.isEmpty()) {
      log.info("No votes cast - no elimination");
      return VotingResult.noElimination();
    }

    // Liczymy TYLKO głosy mafii
    int maxMafiaVotes =
        results.stream().mapToInt(VoteResult::getMafiaVoteCount).max().orElse(0);

    if (maxMafiaVotes == 0) {
      log.info("No mafia votes cast - no elimination");
      return VotingResult.noElimination();
    }

    // Znajdź wszystkich graczy z maksymalną liczbą głosów mafii
    List<VoteResult> topVoted =
        results.stream()
            .filter(r -> r.getMafiaVoteCount() == maxMafiaVotes)
            .collect(Collectors.toList());

    log.info(
        "Top voted players by mafia: {} with {} mafia votes each", topVoted.size(), maxMafiaVotes);

    // Jeśli jest remis - losuj jednego
    if (topVoted.size() > 1) {
      int randomIndex = ThreadLocalRandom.current().nextInt(topVoted.size());
      VoteResult randomWinner = topVoted.get(randomIndex);

      log.info(
          "Tie detected - random elimination: {} (selected from {} candidates)",
          randomWinner.getTargetUser().getUsername(),
          topVoted.size());

      return VotingResult.tieWithRandomElimination(randomWinner.getTargetUser(), topVoted);
    }

    // Jeden zwycięzca - zostaje wyeliminowany
    VoteResult winner = topVoted.getFirst();
    log.info(
        "Player {} eliminated with {} mafia votes",
        winner.getTargetUser().getUsername(),
        winner.getMafiaVoteCount());

    return VotingResult.eliminated(winner.getTargetUser());
  }

  @Override
  public boolean areVotesPublic() {
    // Głosy nocne są tajne - nikt nie widzi kto na kogo głosował
    return false;
  }

  @Override
  public String getStrategyName() {
    return "NIGHT_VOTING";
  }
}
