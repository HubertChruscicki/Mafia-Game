package com.mafia.gameservice.services.voting;

import com.mafia.gameservice.enums.GamePhase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Factory dla tworzenia strategii głosowania
 * Implementuje Factory Pattern dla elastycznego wyboru strategii
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class VotingStrategyFactory {

  private final DayVotingStrategy dayVotingStrategy;
  private final NightVotingStrategy nightVotingStrategy;

  /**
   * Zwraca odpowiednią strategię głosowania dla danej fazy gry
   *
   * @param phase faza gry
   * @return strategia głosowania
   * @throws IllegalArgumentException jeśli nie ma strategii dla danej fazy
   */
  public VotingStrategy getStrategy(GamePhase phase) {
    log.debug("Getting voting strategy for phase: {}", phase);

    VotingStrategy strategy =
        switch (phase) {
          case DAY_VOTE -> {
            log.debug("Selected DAY_VOTING strategy");
            yield dayVotingStrategy;
          }
          case NIGHT_VOTE -> {
            log.debug("Selected NIGHT_VOTING strategy");
            yield nightVotingStrategy;
          }
          case DAY_DISCUSSION, GAME_OVER ->
              throw new IllegalArgumentException(
                  "No voting strategy available for phase: " + phase);
        };

    log.info("Using voting strategy: {}", strategy.getStrategyName());
    return strategy;
  }

  /**
   * Sprawdza czy dla danej fazy istnieje strategia głosowania
   *
   * @param phase faza gry
   * @return true jeśli strategia istnieje
   */
  public boolean hasStrategy(GamePhase phase) {
    return phase == GamePhase.DAY_VOTE || phase == GamePhase.NIGHT_VOTE;
  }
}
