package com.mafia.gameservice.services.voting;

import com.mafia.gameservice.models.VotingSession;
import com.mafia.gameservice.dto.voting.VotingTimerUpdateInd;
import com.mafia.gameservice.enums.VotingStatus;
import com.mafia.gameservice.repositories.VotingSessionRepository;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Serwis zarządzający timerem głosowania
 * Odpowiedzialny za:
 * - Sprawdzanie wygasłych sesji
 * - Broadcast pozostałego czasu przez WebSocket
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VotingTimerService {

  private final VotingSessionRepository votingSessionRepository;
  private final VotingSessionService votingSessionService;
  private final SimpMessagingTemplate messagingTemplate;

  /**
   * Co 5 sekund sprawdza wygasłe sesje
   * Używa @Scheduled z fixedDelay - kolejne wykonanie następuje 5s po zakończeniu poprzedniego
   */
  @Scheduled(fixedDelay = 5000)
  @Transactional
  public void checkExpiredSessions() {
    try {
      LocalDateTime now = LocalDateTime.now();

      List<VotingSession> expiredSessions =
          votingSessionRepository.findByStatusAndEndsAtBefore(VotingStatus.ACTIVE, now);

      if (!expiredSessions.isEmpty()) {
        log.info("Found {} expired voting sessions", expiredSessions.size());

        for (VotingSession session : expiredSessions) {
          try {
            log.info(
                "Expiring voting session: {} (game: {}, phase: {})",
                session.getId(),
                session.getGame().getId(),
                session.getPhase());

            votingSessionService.expireSession(session);
          } catch (Exception e) {
            log.error("Error expiring session {}", session.getId(), e);
          }
        }
      }
    } catch (Exception e) {
      log.error("Error in checkExpiredSessions", e);
    }
  }

  /**
   * Co sekundę wysyła update z pozostałym czasem
   * Broadcast do wszystkich aktywnych sesji
   */
  @Scheduled(fixedDelay = 1000)
  @Transactional(readOnly = true)
  public void broadcastRemainingTime() {
    try {
      List<VotingSession> activeSessions = votingSessionRepository.findByStatus(VotingStatus.ACTIVE);

      for (VotingSession session : activeSessions) {
        try {
          long remainingSeconds =
              ChronoUnit.SECONDS.between(LocalDateTime.now(), session.getEndsAt());

          // Broadcast tylko jeśli czas nie minął
          if (remainingSeconds >= 0) {
            VotingTimerUpdateInd update = new VotingTimerUpdateInd();
            update.setSessionId(session.getId());
            update.setRemainingSeconds(remainingSeconds);

            String topic =
                "/topic/game/" + session.getGame().getRoom().getRoomCode() + "/voting/timer";
            messagingTemplate.convertAndSend(topic, update);

            // Log co 10 sekund aby nie zaśmiecać logów
            if (remainingSeconds % 10 == 0) {
              log.debug(
                  "Timer update for session {}: {}s remaining", session.getId(), remainingSeconds);
            }
          }
        } catch (Exception e) {
          log.error("Error broadcasting timer for session {}", session.getId(), e);
        }
      }
    } catch (Exception e) {
      log.error("Error in broadcastRemainingTime", e);
    }
  }

  /**
   * Co minutę loguje statystyki aktywnych sesji (dla monitoringu)
   */
  @Scheduled(fixedDelay = 60000)
  public void logActiveSessionsStats() {
    try {
      List<VotingSession> activeSessions = votingSessionRepository.findByStatus(VotingStatus.ACTIVE);

      if (!activeSessions.isEmpty()) {
        log.info("Active voting sessions: {}", activeSessions.size());

        for (VotingSession session : activeSessions) {
          long remainingSeconds =
              ChronoUnit.SECONDS.between(LocalDateTime.now(), session.getEndsAt());

          log.info(
              "  Session {}: game={}, phase={}, votes={}/{}, remaining={}s",
              session.getId(),
              session.getGame().getId(),
              session.getPhase(),
              session.getVotesReceived(),
              session.getTotalEligibleVoters(),
              remainingSeconds);
        }
      }
    } catch (Exception e) {
      log.error("Error in logActiveSessionsStats", e);
    }
  }
}
