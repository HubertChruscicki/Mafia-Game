package com.mafia.gameservice.orchestration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mafia.gameservice.enums.GamePhase;
import com.mafia.gameservice.enums.GameRole;
import com.mafia.gameservice.enums.GameRoomStatus;
import com.mafia.gameservice.enums.GameStatus;
import com.mafia.gameservice.models.Game;
import com.mafia.gameservice.models.GamePlayer;
import com.mafia.gameservice.models.GameRoom;
import com.mafia.gameservice.models.User;
import com.mafia.gameservice.models.VotingSession;
import com.mafia.gameservice.repositories.GamePlayerRepository;
import com.mafia.gameservice.repositories.GameRepository;
import com.mafia.gameservice.repositories.GameRoomRepository;
import com.mafia.gameservice.services.GameEventPublisher;
import com.mafia.gameservice.services.VotingResult;
import com.mafia.gameservice.services.VotingSessionService;
import com.mafia.gameservice.statemachine.game.GamePhaseState;
import com.mafia.gameservice.statemachine.game.GamePhaseStateMachine;
import com.mafia.gameservice.support.GameTestFixtures;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoSettings;
import org.springframework.messaging.simp.SimpMessagingTemplate;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GameOrchestratorTest {

    @Spy
    private GamePhaseStateMachine phaseStateMachine = new GamePhaseStateMachine();

    @Mock private GameRepository gameRepository;
    @Mock private GameRoomRepository gameRoomRepository;
    @Mock private GamePlayerRepository gamePlayerRepository;
    @Mock private SimpMessagingTemplate messagingTemplate;
    @Mock private GameEventPublisher eventPublisher;
    @Mock private VotingSessionService votingSessionService;

    @InjectMocks
    private GameOrchestrator orchestrator;

    private Game game;
    private GameRoom room;
    private VotingSession session;

    @BeforeEach
    void setUp() {
        phaseStateMachine.init();
        User host = GameTestFixtures.user("host");
        room = GameTestFixtures.openRoom(host);
        game = GameTestFixtures.activeGame(room);
        session = GameTestFixtures.activeSession(game, GamePhase.NIGHT_VOTE);
        when(gameRepository.save(any(Game.class))).thenAnswer(inv -> inv.getArgument(0));
        when(gameRoomRepository.save(any(GameRoom.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void checkWinConditionsCitizensWinWhenNoMafiaAlive() {
        when(gamePlayerRepository.countByGameAndAssignedRoleAndIsAlive(game, GameRole.MAFIA, true)).thenReturn(0);
        when(gamePlayerRepository.countByGameAndIsAlive(game, true)).thenReturn(4);

        GameOrchestrator.WinConditionResult result = orchestrator.checkWinConditions(game);

        assertThat(result.hasWinner()).isTrue();
        assertThat(result.getWinner()).isEqualTo("CITIZENS");
    }

    @Test
    void checkWinConditionsMafiaWinWhenMafiaEqualsCitizens() {
        when(gamePlayerRepository.countByGameAndAssignedRoleAndIsAlive(game, GameRole.MAFIA, true)).thenReturn(2);
        when(gamePlayerRepository.countByGameAndIsAlive(game, true)).thenReturn(4);

        GameOrchestrator.WinConditionResult result = orchestrator.checkWinConditions(game);

        assertThat(result.hasWinner()).isTrue();
        assertThat(result.getWinner()).isEqualTo("MAFIA");
    }

    @Test
    void checkWinConditionsNoWinnerDuringGame() {
        when(gamePlayerRepository.countByGameAndAssignedRoleAndIsAlive(game, GameRole.MAFIA, true)).thenReturn(1);
        when(gamePlayerRepository.countByGameAndIsAlive(game, true)).thenReturn(5);

        GameOrchestrator.WinConditionResult result = orchestrator.checkWinConditions(game);

        assertThat(result.hasWinner()).isFalse();
    }

    @Test
    void handleVotingCompletedStartsNextPhaseWhenNoWinner() {
        when(gamePlayerRepository.countByGameAndAssignedRoleAndIsAlive(game, GameRole.MAFIA, true)).thenReturn(1);
        when(gamePlayerRepository.countByGameAndIsAlive(game, true)).thenReturn(4);

        orchestrator.handleVotingCompleted(session, VotingResult.noElimination());

        assertThat(game.getCurrentPhase()).isEqualTo(GamePhase.DAY_VOTE);
        verify(votingSessionService).startVotingSession(game, GamePhase.DAY_VOTE);
        verify(eventPublisher).publishPhaseChanged(eq(game), any(), any());
    }

    @Test
    void handleVotingCompletedEndsGameWhenCitizensWin() {
        User victim = GameTestFixtures.user("victim");
        GamePlayer eliminated = GameTestFixtures.alivePlayer(game, victim, GameRole.MAFIA);
        when(gamePlayerRepository.countByGameAndAssignedRoleAndIsAlive(game, GameRole.MAFIA, true)).thenReturn(0);
        when(gamePlayerRepository.countByGameAndIsAlive(game, true)).thenReturn(3);
        when(gamePlayerRepository.findAllByGameId(game.getId())).thenReturn(List.of(eliminated));

        orchestrator.handleVotingCompleted(session, VotingResult.elimination(victim, List.of()));

        assertThat(game.getStatus()).isEqualTo(GameStatus.FINISHED);
        assertThat(game.getCurrentPhase()).isEqualTo(GamePhase.GAME_OVER);
        assertThat(game.getWinnerTeam()).isEqualTo("CITIZENS");
        assertThat(room.getGameRoomStatus()).isEqualTo(GameRoomStatus.OPEN);
        verify(eventPublisher).publishGameEnded(eq(game), eq("CITIZENS"), any(), any());
        verify(votingSessionService, never()).startVotingSession(any(), any());
    }

    @Test
    void handleVotingExpiredDelegatesToVotingSessionService() {
        orchestrator.handleVotingExpired(session);

        verify(votingSessionService).expireSession(session);
    }

    @Test
    void startNextPhaseIncrementsDayWhenEnteringDayVoting() {
        game.setCurrentPhase(GamePhase.NIGHT_RESULT);

        orchestrator.startNextPhase(game);

        assertThat(game.getCurrentDayNumber()).isEqualTo(2);
        assertThat(phaseStateMachine.getCurrentState(game)).isEqualTo(GamePhaseState.DAY_VOTING);
        verify(votingSessionService).startVotingSession(game, GamePhase.DAY_VOTE);
    }
}
