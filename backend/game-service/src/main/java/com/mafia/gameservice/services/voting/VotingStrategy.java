package com.mafia.gameservice.services.voting;

import com.mafia.gameservice.models.GamePlayer;
import com.mafia.gameservice.models.GameVote;
import com.mafia.gameservice.models.VoteResult;
import com.mafia.gameservice.models.VotingSession;
import java.util.List;

/**
 * Strategia głosowania - definiuje zachowanie dla różnych typów głosowań
 * Implementuje Strategy Pattern dla elastycznego zarządzania logiką głosowania
 */
public interface VotingStrategy {

  /**
   * Waliduje czy głos jest ważny
   *
   * @param vote głos do walidacji
   * @param voter gracz oddający głos
   * @return true jeśli głos jest ważny
   */
  boolean validateVote(GameVote vote, GamePlayer voter);

  /**
   * Liczy głosy mafii (tylko dla NIGHT_VOTE)
   *
   * @param votes lista głosów
   * @param session sesja głosowania
   * @return liczba głosów mafii
   */
  int countMafiaVotes(List<GameVote> votes, VotingSession session);

  /**
   * Określa wynik głosowania na podstawie zagregowanych wyników
   *
   * @param results zagregowane wyniki głosowania
   * @param session sesja głosowania
   * @return wynik głosowania z informacją o eliminowanym graczu
   */
  VotingResult determineResult(List<VoteResult> results, VotingSession session);

  /**
   * Czy głosy są publiczne (widoczne dla wszystkich)
   *
   * @return true jeśli głosy są publiczne
   */
  boolean areVotesPublic();

  /**
   * Nazwa strategii (dla logowania)
   *
   * @return nazwa strategii
   */
  String getStrategyName();
}
