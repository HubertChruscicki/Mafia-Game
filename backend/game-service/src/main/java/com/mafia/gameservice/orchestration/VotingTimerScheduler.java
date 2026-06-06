package com.mafia.gameservice.orchestration;
import com.mafia.gameservice.enums.VotingStatus;
import com.mafia.gameservice.models.VotingSession;
import com.mafia.gameservice.repositories.VotingSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

/**
 * Scheduler odpowiedzialny za:
 * 1. Automatyczne kończenie sesji głosowania po upływie czasu
 * 2. Broadcast timer ticks do klientów (synchronizacja czasu)
 *
 * Działa co sekundę, sprawdzając aktywne sesje głosowania.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class VotingTimerScheduler {

    private final VotingSessionRepository votingSessionRepository;
    private final GameOrchestrator gameOrchestrator;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Sprawdza co sekundę czy są sesje do zakończenia.
     * Automatycznie kończy sesje których czas minął.
     */
    @Scheduled(fixedRate = 1000)
    @Transactional
    public void checkExpiredSessions() {
        LocalDateTime now = LocalDateTime.now();

        // Znajdź aktywne sesje które powinny się zakończyć
        List<VotingSession> expiredSessions = votingSessionRepository
                .findByStatusAndEndsAtBefore(VotingStatus.ACTIVE, now);

        for (VotingSession session : expiredSessions) {
            try {
                log.info("⏰ Auto-expiring voting session {} (ended at: {}, now: {})",
                        session.getId(), session.getEndsAt(), now);

                gameOrchestrator.handleVotingExpired(session);
            } catch (Exception e) {
                log.error("Error auto-expiring session {}: {}", session.getId(), e.getMessage(), e);
            }
        }
    }

    /**
     * Wysyła tick timera co sekundę dla aktywnych sesji.
     * readOnly transaction keeps lazy associations (game.room) initialized.
     */
    @Scheduled(fixedRate = 1000)
    @Transactional(readOnly = true)
    public void broadcastTimerTicks() {
        try {
            List<VotingSession> activeSessions = votingSessionRepository.findByStatus(VotingStatus.ACTIVE);

            for (VotingSession session : activeSessions) {
                try {
                    long remainingSeconds = ChronoUnit.SECONDS.between(
                            LocalDateTime.now(), session.getEndsAt());

                    // Tylko wysyłaj jeśli czas >= 0
                    if (remainingSeconds >= 0) {
                        String roomCode = session.getGame().getRoom().getRoomCode();

                        Map<String, Object> timerUpdate = Map.of(
                                "sessionId", session.getId(),
                                "remainingSeconds", remainingSeconds,
                                "phase", session.getPhase().name(),
                                "dayNumber", session.getDayNumber(),
                                "votesReceived", session.getVotesReceived(),
                                "totalVoters", session.getTotalEligibleVoters()
                        );

                        String topic = "/topic/game/" + roomCode + "/voting/timer";
                        messagingTemplate.convertAndSend(topic, (Object) timerUpdate);

                        // Loguj tylko co 10 sekund aby nie zaśmiecać logów
                        if (remainingSeconds % 10 == 0) {
                            log.debug("Timer tick for session {}: {}s remaining",
                                    session.getId(), remainingSeconds);
                        }
                    }
                } catch (Exception e) {
                    log.error("Error broadcasting timer for session {}: {}",
                            session.getId(), e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Error in broadcastTimerTicks: {}", e.getMessage());
        }
    }
}
