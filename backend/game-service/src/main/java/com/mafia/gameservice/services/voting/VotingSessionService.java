package com.mafia.gameservice.services.voting;

import com.mafia.gameservice.models.*;
import com.mafia.gameservice.dto.voting.*;
import com.mafia.gameservice.enums.GamePhase;
import com.mafia.gameservice.enums.VotingStatus;
import com.mafia.gameservice.repositories.*;
import com.mafia.gameservice.services.GameEventPublisher;
import com.mafia.gameservice.exceptions.VotingSessionNotFoundException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Serwis zarządzający sesjami głosowania
 * Odpowiedzialny za:
 * - Tworzenie sesji głosowania
 * - Przyjmowanie głosów
 * - Aktualizację wyników
 * - Zakończenie głosowania
 * - Broadcast przez WebSocket
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VotingSessionService {

  private final VotingSessionRepository votingSessionRepository;
  private final VoteResultRepository voteResultRepository;
  private final GameVoteRepository gameVoteRepository;
  private final GamePlayerRepository gamePlayerRepository;
  private final UserRepository userRepository;
  private final SimpMessagingTemplate messagingTemplate;
  private final VotingStrategyFactory votingStrategyFactory;
  private final GameRepository gameRepository;
  private final GameEventPublisher eventPublisher;

  /**
   * Rozpoczyna nową sesję głosowania
   */
  @Transactional
  public VotingSession startVotingSession(Game game, GamePhase phase, int discussionTimeSeconds) {
    log.info(
        "Starting voting session for game {} phase {} day {}",
        game.getId(),
        phase,
        game.getCurrentDayNumber());

    // Sprawdź czy nie ma aktywnej sesji
    Optional<VotingSession> existing =
        votingSessionRepository.findByGameAndPhaseAndDayNumberAndStatus(
            game, phase, game.getCurrentDayNumber(), VotingStatus.ACTIVE);

    if (existing.isPresent()) {
      log.warn("Active voting session already exists for game {}", game.getId());
      throw new IllegalStateException("Active voting session already exists");
    }

    // Policz żywych graczy
    int aliveCount = gamePlayerRepository.countByGameAndIsAlive(game, true);
    log.info("Alive players count: {}", aliveCount);

    if (aliveCount == 0) {
      throw new IllegalStateException("No alive players to vote");
    }

    VotingSession session = new VotingSession();
    session.setGame(game);
    session.setPhase(phase);
    session.setDayNumber(game.getCurrentDayNumber());
    session.setStartedAt(LocalDateTime.now());
    session.setEndsAt(LocalDateTime.now().plusSeconds(discussionTimeSeconds));
    session.setStatus(VotingStatus.ACTIVE);
    session.setTotalEligibleVoters(aliveCount);
    session.setVotesReceived(0);

    VotingSession saved = votingSessionRepository.save(session);
    log.info("Voting session created: {}", saved.getId());

    // Wyślij WebSocket notification
    broadcastVotingUpdate(saved);

    return saved;
  }

  /**
   * Oddanie głosu
   */
  @Transactional
  public CastVoteResponse castVote(UUID sessionId, UUID voterId, UUID targetUserId) {
    log.info("Casting vote: session={}, voter={}, target={}", sessionId, voterId, targetUserId);

    Optional<VotingSession> sessionOpt = votingSessionRepository.findById(sessionId);
    if (sessionOpt.isEmpty()) {
      return CastVoteResponse.error("Voting session not found");
    }
    VotingSession session = sessionOpt.get();

    // Walidacja statusu sesji
    if (session.getStatus() != VotingStatus.ACTIVE) {
      log.warn("Voting session {} is not active: {}", sessionId, session.getStatus());
      return CastVoteResponse.error("Voting session is not active");
    }

    // Walidacja czasu
    if (LocalDateTime.now().isAfter(session.getEndsAt())) {
      log.warn("Voting session {} has expired", sessionId);
      expireSession(session);
      return CastVoteResponse.error("Voting time has expired");
    }

    // Znajdź głosującego
    GamePlayer voter =
        gamePlayerRepository
            .findByGameAndUser_Id(session.getGame(), voterId)
            .orElseThrow(() -> new IllegalArgumentException("Voter not found in game"));

    // Sprawdź czy gracz żyje
    if (!voter.isAlive()) {
      log.warn("Dead player {} attempted to vote", voterId);
      return CastVoteResponse.error("Dead players cannot vote");
    }

    // Sprawdź czy już głosował
    Optional<GameVote> existingVote =
        gameVoteRepository.findByVotingSessionAndVoterId(session, voterId);

    if (existingVote.isPresent()) {
      log.warn("Player {} already voted in session {}", voterId, sessionId);
      return CastVoteResponse.error("You have already voted");
    }

    // Zapisz głos
    GameVote vote = new GameVote();
    vote.setGame(session.getGame());
    vote.setVotingSession(session);
    vote.setPhase(session.getPhase());
    vote.setDayNumber(session.getDayNumber());
    vote.setVoterId(voterId);
    vote.setTargetUserId(targetUserId);
    vote.setVotedAt(LocalDateTime.now());

    // Użyj strategii do walidacji głosu
    VotingStrategy strategy = votingStrategyFactory.getStrategy(session.getPhase());
    vote.setValid(strategy.validateVote(vote, voter));

    gameVoteRepository.save(vote);
    log.info("Vote saved: {}", vote.getId());

    // Publikuj zdarzenie głosu do RabbitMQ
    User voterUser = voter.getUser();
    User targetUser = userRepository.findById(targetUserId).orElse(null);
    if (targetUser != null) {
      eventPublisher.publishVoteCast(
          session.getGame(),
          voterUser,
          voter.getAssignedRole().name(),
          targetUser,
          session.getPhase().name(),
          session.getVotesReceived() + 1,
          session.getTotalEligibleVoters()
      );
    }

    // Aktualizuj licznik
    session.setVotesReceived(session.getVotesReceived() + 1);
    votingSessionRepository.save(session);

    // Aktualizuj wyniki
    updateVoteResults(session);

    // Broadcast update
    broadcastVotingUpdate(session);

    // Sprawdź czy wszyscy zagłosowali
    if (session.getVotesReceived() >= session.getTotalEligibleVoters()) {
      log.info("All players voted - completing session {}", sessionId);
      completeVoting(session);
    }

    return CastVoteResponse.success(toDto(session));
  }

  /**
   * Aktualizuje agregowane wyniki głosowania
   */
  @Transactional
  public void updateVoteResults(VotingSession session) {
    log.debug("Updating vote results for session {}", session.getId());

    List<GameVote> votes = gameVoteRepository.findByVotingSessionAndIsValid(session, true);

    // Grupuj głosy po target
    Map<UUID, List<GameVote>> votesByTarget =
        votes.stream().collect(Collectors.groupingBy(GameVote::getTargetUserId));

    VotingStrategy strategy = votingStrategyFactory.getStrategy(session.getPhase());

    for (Map.Entry<UUID, List<GameVote>> entry : votesByTarget.entrySet()) {
      UUID targetId = entry.getKey();
      List<GameVote> targetVotes = entry.getValue();

      VoteResult result =
          voteResultRepository
              .findByVotingSessionAndTargetUser_Id(session, targetId)
              .orElse(new VoteResult());

      result.setVotingSession(session);
      result.setTargetUser(userRepository.findById(targetId).orElse(null));
      result.setVoteCount(targetVotes.size());

      // Dla nocnego głosowania - licz głosy mafii
      if (session.getPhase() == GamePhase.NIGHT_VOTE) {
        int mafiaVotes = strategy.countMafiaVotes(targetVotes, session);
        result.setMafiaVoteCount(mafiaVotes);
        log.debug("Target {} has {} mafia votes", targetId, mafiaVotes);
      }

      result.setUpdatedAt(LocalDateTime.now());
      voteResultRepository.save(result);
    }

    log.debug("Vote results updated for {} targets", votesByTarget.size());
  }

  /**
   * Zakończenie głosowania
   */
  @Transactional
  public void completeVoting(VotingSession session) {
    log.info("Completing voting session {}", session.getId());

    VotingStrategy strategy = votingStrategyFactory.getStrategy(session.getPhase());

    // Pobierz wyniki
    List<VoteResult> results = voteResultRepository.findByVotingSession(session);

    // Określ eliminowanego gracza
    VotingResult votingResult = strategy.determineResult(results, session);
    log.info("Voting result: {}", votingResult.getDescription());

    session.setStatus(VotingStatus.COMPLETED);
    session.setResultUser(votingResult.getEliminatedUser());
    session.setTie(votingResult.isTie());
    votingSessionRepository.save(session);

    // Jeśli ktoś został wyeliminowany
    if (votingResult.hasElimination()) {
      GamePlayer eliminated =
          gamePlayerRepository
              .findByGameAndUser(session.getGame(), votingResult.getEliminatedUser())
              .orElseThrow(() -> new IllegalStateException("Eliminated player not found"));

      eliminated.setAlive(false);
      gamePlayerRepository.save(eliminated);
      log.info(
          "Player {} eliminated",
          votingResult.getEliminatedUser() != null
              ? votingResult.getEliminatedUser().getUsername()
              : "unknown");
    }

    // Broadcast final result
    broadcastVotingComplete(session, votingResult);

    // Sprawdź warunki wygranej po eliminacji
    checkGameEndConditions(session.getGame());
  }

  /**
   * Sprawdza warunki wygranej i kończy grę jeśli spełnione.
   * Jeśli gra trwa dalej, rozpoczyna następną fazę.
   */
  private void checkGameEndConditions(Game game) {
    try {
      // Policz żywych graczy według ról
      int aliveMafia =
          gamePlayerRepository.countByGameAndAssignedRoleAndIsAlive(
              game, com.mafia.gameservice.enums.GameRole.MAFIA, true);

      int totalAlive = gamePlayerRepository.countByGameAndIsAlive(game, true);
      int aliveCitizens = totalAlive - aliveMafia;

      log.info(
          "Game {} status: {} alive mafia, {} alive citizens",
          game.getId(),
          aliveMafia,
          aliveCitizens);

      String winner = null;

      // Warunek wygranej Obywateli: wszystkie mafie wyeliminowane
      if (aliveMafia == 0) {
        log.info("Citizens win! All mafia eliminated in game {}", game.getId());
        winner = "CITIZENS";
      }
      // Warunek wygranej Mafii: mafia >= obywatele
      else if (aliveMafia >= aliveCitizens) {
        log.info("Mafia wins! Mafia count >= citizens in game {}", game.getId());
        winner = "MAFIA";
      }

      // Jeśli jest zwycięzca, zakończ grę
      if (winner != null) {
        game.setStatus(com.mafia.gameservice.enums.GameStatus.FINISHED);
        game.setCurrentPhase(GamePhase.GAME_OVER);
        game.setEndedAt(LocalDateTime.now());
        gameRepository.save(game);

        // Aktualizuj status pokoju na OPEN (gotowy do nowej gry)
        GameRoom room = game.getRoom();
        room.setGameRoomStatus(com.mafia.gameservice.enums.GameRoomStatus.OPEN);

        // Broadcast game over
        broadcastGameOver(game, winner);

        // Broadcast room status update - pokój wraca do stanu OPEN
        broadcastRoomStatusAfterGameEnd(room);

        log.info("Game {} ended. Winner: {}. Room {} status set to OPEN", 
            game.getId(), winner, room.getRoomCode());
      } else {
        // Gra trwa dalej - rozpocznij następną fazę
        log.info("Game {} continues - starting next phase", game.getId());
        startNextPhase(game);
      }
    } catch (Exception e) {
      log.error("Error checking game end conditions", e);
    }
  }

  /**
   * Rozpoczyna następną fazę gry.
   * NIGHT_VOTE → DAY_VOTE �� NIGHT_VOTE → ...
   * 
   * Numerowanie dni:
   * - Gra zaczyna się od NIGHT_VOTE dzień 1
   * - Po NIGHT_VOTE dzień 1 → DAY_VOTE dzień 1 (ten sam dzień)
   * - Po DAY_VOTE dzień 1 → NIGHT_VOTE dzień 2 (nowy dzień)
   */
  private void startNextPhase(Game game) {
    GamePhase currentPhase = game.getCurrentPhase();
    GamePhase nextPhase;
    
    if (currentPhase == GamePhase.NIGHT_VOTE) {
      // Po nocy przechodzi dzień (ten sam numer dnia)
      nextPhase = GamePhase.DAY_VOTE;
      log.info("Transitioning to DAY_VOTE, day {} (same day)", game.getCurrentDayNumber());
    } else {
      // Po dniu przechodzi noc (nowy dzień)
      nextPhase = GamePhase.NIGHT_VOTE;
      game.setCurrentDayNumber(game.getCurrentDayNumber() + 1);
      log.info("Transitioning to NIGHT_VOTE, day {} (new day)", game.getCurrentDayNumber());
    }
    
    game.setCurrentPhase(nextPhase);
    gameRepository.save(game);
    
    // Broadcast zmianę fazy
    broadcastPhaseChange(game, nextPhase);
    
    // Rozpocznij nową sesję głosowania
    int discussionTime = game.getRoom().getDiscussionTimeSeconds();
    startVotingSession(game, nextPhase, discussionTime);
  }

  /**
   * Broadcast zmiany fazy gry
   */
  private void broadcastPhaseChange(Game game, GamePhase newPhase) {
    try {
      Map<String, Object> payload = new HashMap<>();
      payload.put("type", "phase_change");
      payload.put("gameId", game.getId());
      payload.put("phase", newPhase.name());
      payload.put("dayNumber", game.getCurrentDayNumber());
      
      String topic = "/topic/game/" + game.getRoom().getRoomCode() + "/phase/change";
      messagingTemplate.convertAndSend(topic, (Object) payload);
      
      log.info("Broadcast phase change to {}: {}", topic, newPhase);
    } catch (Exception e) {
      log.error("Error broadcasting phase change", e);
    }
  }

  /**
   * Broadcast zakończenia gry
   */
  private void broadcastGameOver(Game game, String winner) {
    try {
      Map<String, Object> gameOverData = new HashMap<>();
      gameOverData.put("gameId", game.getId());
      gameOverData.put("winner", winner);
      gameOverData.put("totalDays", game.getCurrentDayNumber());
      gameOverData.put("endedAt", game.getEndedAt());

      // Pobierz wszystkich graczy z ich rolami
      List<GamePlayer> players = gamePlayerRepository.findAllByGameId(game.getId());
      List<Map<String, Object>> playerData =
          players.stream()
              .map(
                  p -> {
                    Map<String, Object> data = new HashMap<>();
                    data.put("userId", p.getUser().getId());
                    data.put("username", p.getUser().getUsername());
                    data.put("role", p.getAssignedRole().name());
                    data.put("isAlive", p.isAlive());
                    return data;
                  })
              .collect(Collectors.toList());

      gameOverData.put("players", playerData);

      String topic = "/topic/game/" + game.getRoom().getRoomCode() + "/gameOver";
      messagingTemplate.convertAndSend(topic, (Object) gameOverData);

      log.info("Broadcast game over to topic: {}", topic);
    } catch (Exception e) {
      log.error("Error broadcasting game over", e);
    }
  }

  /**
   * Wygaśnięcie sesji (timeout)
   * Zawsze przechodzi do następnej fazy - niezależnie czy były głosy czy nie.
   */
  @Transactional
  public void expireSession(VotingSession session) {
    log.info("Expiring voting session {}", session.getId());

    // Sprawdź czy sesja nie została już przetworzona
    if (session.getStatus() != VotingStatus.ACTIVE) {
      log.warn("Session {} already processed with status {}", session.getId(), session.getStatus());
      return;
    }

    session.setStatus(VotingStatus.EXPIRED);
    votingSessionRepository.save(session);

    // Jeśli były jakieś głosy, przetwórz je normalnie
    long voteCount = gameVoteRepository.countByVotingSession(session);
    if (voteCount > 0) {
      log.info("Processing {} votes before expiration", voteCount);
      // completeVoting już obsługuje przejście do następnej fazy
      completeVoting(session);
    } else {
      // Brak głosów - broadcast expired i przejdź do następnej fazy
      log.info("No votes cast - proceeding to next phase");
      broadcastVotingExpired(session);
      
      // Przejdź do następnej fazy (bez eliminacji)
      Game game = session.getGame();
      checkGameEndConditions(game);
    }
  }

  /**
   * Pobierz aktywną sesję dla gry
   */
  @Transactional(readOnly = true)
  public Optional<VotingSession> getCurrentActiveSession(Game game) {
    return votingSessionRepository.findActiveSessionByGame(game);
  }

  /**
   * Pobierz wyniki głosowania
   */
  @Transactional(readOnly = true)
  public List<VoteResultDto> getResults(UUID sessionId) {
    VotingSession session =
        votingSessionRepository
            .findById(sessionId)
            .orElseThrow(() -> new VotingSessionNotFoundException("Voting session not found"));

    return toResultDtos(session);
  }

  // ==================== HELPER METHODS ====================

  /**
   * Konwertuje VotingSession do DTO
   */
  public VotingSessionDto toDto(VotingSession session) {
    VotingSessionDto dto = new VotingSessionDto();
    dto.setSessionId(session.getId());
    dto.setGameId(session.getGame().getId());
    dto.setRoomCode(session.getGame().getRoom().getRoomCode());
    dto.setPhase(session.getPhase());
    dto.setDayNumber(session.getDayNumber());
    dto.setStartedAt(session.getStartedAt());
    dto.setEndsAt(session.getEndsAt());
    dto.setTotalEligibleVoters(session.getTotalEligibleVoters());
    dto.setVotesReceived(session.getVotesReceived());
    dto.setStatus(session.getStatus());
    dto.setCurrentResults(toResultDtos(session));

    // Oblicz pozostały czas
    long remainingSeconds =
        ChronoUnit.SECONDS.between(LocalDateTime.now(), session.getEndsAt());
    dto.setRemainingTimeSeconds(Math.max(0, remainingSeconds));

    return dto;
  }

  /**
   * Konwertuje wyniki głosowania do DTO
   */
  private List<VoteResultDto> toResultDtos(VotingSession session) {
    List<VoteResult> results = voteResultRepository.findByVotingSession(session);
    VotingStrategy strategy = votingStrategyFactory.getStrategy(session.getPhase());
    boolean showVoters = strategy.areVotesPublic();

    return results.stream()
        .map(
            result -> {
              VoteResultDto dto = new VoteResultDto();
              dto.setTargetUserId(result.getTargetUser().getId());
              dto.setTargetUsername(result.getTargetUser().getUsername());
              dto.setVoteCount(result.getVoteCount());
              dto.setMafiaVoteCount(result.getMafiaVoteCount());

              // Dodaj listę głosujących tylko dla publicznych głosowań
              if (showVoters) {
                dto.setVoters(getVotersForTarget(session, result.getTargetUser().getId()));
              } else {
                dto.setVoters(List.of());
              }

              return dto;
            })
        .collect(Collectors.toList());
  }

  /**
   * Pobiera listę głosujących na danego gracza
   */
  private List<VoterDto> getVotersForTarget(VotingSession session, UUID targetUserId) {
    List<GameVote> votes = gameVoteRepository.findByVotingSessionAndIsValid(session, true);

    return votes.stream()
        .filter(vote -> vote.getTargetUserId().equals(targetUserId))
        .map(
            vote -> {
              User voter = userRepository.findById(vote.getVoterId()).orElse(null);
              return new VoterDto(
                  vote.getVoterId(),
                  voter != null ? voter.getUsername() : "Unknown",
                  vote.getVotedAt());
            })
        .collect(Collectors.toList());
  }

  // ==================== WEBSOCKET BROADCASTS ====================

  /**
   * Broadcast aktualizacji głosowania
   */
  private void broadcastVotingUpdate(VotingSession session) {
    try {
      VotingUpdateInd update = new VotingUpdateInd();
      update.setSessionId(session.getId());
      update.setVotesReceived(session.getVotesReceived());
      update.setTotalEligibleVoters(session.getTotalEligibleVoters());
      update.setCurrentResults(toResultDtos(session));
      update.setStatus(session.getStatus());

      long remainingSeconds =
          ChronoUnit.SECONDS.between(LocalDateTime.now(), session.getEndsAt());
      update.setRemainingTimeSeconds(Math.max(0, remainingSeconds));

      String topic = "/topic/game/" + session.getGame().getRoom().getRoomCode() + "/voting";
      messagingTemplate.convertAndSend(topic, (Object) update);

      log.debug("Broadcast voting update to topic: {}", topic);
    } catch (Exception e) {
      log.error("Error broadcasting voting update", e);
    }
  }

  /**
   * Broadcast zakończenia głosowania
   */
  private void broadcastVotingComplete(VotingSession session, VotingResult result) {
    try {
      VotingCompleteInd indication = new VotingCompleteInd();
      indication.setSessionId(session.getId());
      indication.setEliminatedUserId(
          result.getEliminatedUser() != null ? result.getEliminatedUser().getId() : null);
      indication.setEliminatedUsername(
          result.getEliminatedUser() != null ? result.getEliminatedUser().getUsername() : null);
      indication.setTie(result.isTie());
      indication.setResultType(result.getResultType().name());
      indication.setFinalResults(toResultDtos(session));

      String topic = "/topic/game/" + session.getGame().getRoom().getRoomCode() + "/voting/complete";
      messagingTemplate.convertAndSend(topic, (Object) indication);

      log.info("Broadcast voting complete to topic: {}", topic);
    } catch (Exception e) {
      log.error("Error broadcasting voting complete", e);
    }
  }

  /**
   * Broadcast wygaśnięcia sesji
   */
  private void broadcastVotingExpired(VotingSession session) {
    try {
      VotingCompleteInd indication = new VotingCompleteInd();
      indication.setSessionId(session.getId());
      indication.setEliminatedUserId(null);
      indication.setEliminatedUsername(null);
      indication.setTie(false);
      indication.setResultType("EXPIRED_NO_VOTES");
      indication.setFinalResults(List.of());

      String topic = "/topic/game/" + session.getGame().getRoom().getRoomCode() + "/voting/complete";
      messagingTemplate.convertAndSend(topic, (Object) indication);

      log.info("Broadcast voting expired to topic: {}", topic);
    } catch (Exception e) {
      log.error("Error broadcasting voting expired", e);
    }
  }

  /**
   * Broadcast zmiany statusu pokoju po zakończeniu gry
   * Informuje wszystkich graczy, że pokój wrócił do stanu OPEN
   */
  private void broadcastRoomStatusAfterGameEnd(GameRoom room) {
    try {
      Map<String, Object> update = new HashMap<>();
      update.put("type", "room_status_changed");
      update.put("roomCode", room.getRoomCode());
      update.put("status", room.getGameRoomStatus().name());
      update.put("message", "Game ended. Room is now open for a new game.");

      String topic = "/topic/game/" + room.getRoomCode() + "/updated";
      messagingTemplate.convertAndSend(topic, (Object) update);

      log.info("Broadcast room status update after game end to topic: {} with status: {}", 
          topic, room.getGameRoomStatus());
    } catch (Exception e) {
      log.error("Error broadcasting room status after game end for room: {}", room.getRoomCode(), e);
    }
  }
}
