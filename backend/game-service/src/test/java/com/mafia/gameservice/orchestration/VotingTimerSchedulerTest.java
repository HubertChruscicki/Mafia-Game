package com.mafia.gameservice.orchestration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mafia.gameservice.enums.VotingStatus;
import com.mafia.gameservice.models.VotingSession;
import com.mafia.gameservice.repositories.VotingSessionRepository;
import com.mafia.gameservice.support.GameTestFixtures;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

@ExtendWith(MockitoExtension.class)
class VotingTimerSchedulerTest {

    @Mock private VotingSessionRepository votingSessionRepository;
    @Mock private GameOrchestrator gameOrchestrator;
    @Mock private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private VotingTimerScheduler scheduler;

    @Test
    void checkExpiredSessionsDelegatesToOrchestrator() {
        VotingSession session = GameTestFixtures.activeSession(
                GameTestFixtures.activeGame(GameTestFixtures.openRoom(GameTestFixtures.user("host"))),
                com.mafia.gameservice.enums.GamePhase.DAY_VOTE);
        session.setEndsAt(LocalDateTime.now().minusSeconds(1));

        when(votingSessionRepository.findByStatusAndEndsAtBefore(
                org.mockito.ArgumentMatchers.eq(VotingStatus.ACTIVE), any(LocalDateTime.class)))
                .thenReturn(List.of(session));

        scheduler.checkExpiredSessions();

        verify(gameOrchestrator).handleVotingExpired(session);
    }

    @Test
    void broadcastTimerTicksPublishesRemainingTime() {
        VotingSession session = GameTestFixtures.activeSession(
                GameTestFixtures.activeGame(GameTestFixtures.openRoom(GameTestFixtures.user("host"))),
                com.mafia.gameservice.enums.GamePhase.DAY_VOTE);
        session.setEndsAt(LocalDateTime.now().plusSeconds(30));

        when(votingSessionRepository.findByStatus(VotingStatus.ACTIVE)).thenReturn(List.of(session));

        scheduler.broadcastTimerTicks();

        verify(messagingTemplate).convertAndSend(
                org.mockito.ArgumentMatchers.eq("/topic/game/ABC123/voting/timer"),
                org.mockito.ArgumentMatchers.<Object>any());
    }
}
