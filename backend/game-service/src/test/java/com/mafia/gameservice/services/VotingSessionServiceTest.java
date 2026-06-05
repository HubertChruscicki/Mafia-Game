package com.mafia.gameservice.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mafia.gameservice.dto.voting.CastVoteResponse;
import com.mafia.gameservice.enums.GamePhase;
import com.mafia.gameservice.enums.GameRole;
import com.mafia.gameservice.enums.VotingStatus;
import com.mafia.gameservice.models.Game;
import com.mafia.gameservice.models.GamePlayer;
import com.mafia.gameservice.models.GameRoom;
import com.mafia.gameservice.models.GameVote;
import com.mafia.gameservice.models.User;
import com.mafia.gameservice.models.VotingSession;
import com.mafia.gameservice.orchestration.GameOrchestrator;
import com.mafia.gameservice.repositories.GamePlayerRepository;
import com.mafia.gameservice.repositories.GameRepository;
import com.mafia.gameservice.repositories.GameVoteRepository;
import com.mafia.gameservice.repositories.UserRepository;
import com.mafia.gameservice.repositories.VotingSessionRepository;
import com.mafia.gameservice.support.GameTestFixtures;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

@ExtendWith(MockitoExtension.class)
class VotingSessionServiceTest {

    @Mock private VotingSessionRepository votingSessionRepository;
    @Mock private GameVoteRepository gameVoteRepository;
    @Mock private GamePlayerRepository gamePlayerRepository;
    @Mock private UserRepository userRepository;
    @Mock private GameRepository gameRepository;
    @Mock private SimpMessagingTemplate messagingTemplate;
    @Mock private GameOrchestrator gameOrchestrator;

    @InjectMocks
    private VotingSessionService votingSessionService;

    private Game game;
    private GameRoom room;
    private VotingSession session;
    private User voter;
    private User target;
    private GamePlayer voterPlayer;
    private GamePlayer targetPlayer;

    @BeforeEach
    void setUp() {
        User host = GameTestFixtures.user("host");
        room = GameTestFixtures.openRoom(host);
        game = GameTestFixtures.activeGame(room);
        session = GameTestFixtures.activeSession(game, GamePhase.DAY_VOTE);
        voter = GameTestFixtures.user("voter");
        target = GameTestFixtures.user("target");
        voterPlayer = GameTestFixtures.alivePlayer(game, voter, GameRole.CITIZEN);
        targetPlayer = GameTestFixtures.alivePlayer(game, target, GameRole.CITIZEN);
    }

    @Test
    void startVotingSessionCreatesActiveSession() {
        when(votingSessionRepository.findByGameAndPhaseAndDayNumberAndStatus(
                game, GamePhase.NIGHT_VOTE, 1, VotingStatus.ACTIVE)).thenReturn(Optional.empty());
        when(gamePlayerRepository.countByGameAndIsAlive(game, true)).thenReturn(3);
        when(votingSessionRepository.save(any(VotingSession.class))).thenAnswer(inv -> {
            VotingSession saved = inv.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });

        VotingSession created = votingSessionService.startVotingSession(game, GamePhase.NIGHT_VOTE);

        assertThat(created.getStatus()).isEqualTo(VotingStatus.ACTIVE);
        assertThat(created.getTotalEligibleVoters()).isEqualTo(3);
        verify(messagingTemplate).convertAndSend(eq("/topic/game/ABC123/voting"), org.mockito.ArgumentMatchers.<Object>any());
    }

    @Test
    void startVotingSessionRejectsDuplicateActiveSession() {
        when(votingSessionRepository.findByGameAndPhaseAndDayNumberAndStatus(
                game, GamePhase.NIGHT_VOTE, 1, VotingStatus.ACTIVE))
                .thenReturn(Optional.of(session));

        assertThatThrownBy(() -> votingSessionService.startVotingSession(game, GamePhase.NIGHT_VOTE))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void castVoteRejectsUnknownSession() {
        when(votingSessionRepository.findById(session.getId())).thenReturn(Optional.empty());

        CastVoteResponse response = votingSessionService.castVote(session.getId(), voter.getId(), target.getId());

        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getMessage()).contains("not found");
    }

    @Test
    void castVoteRejectsDeadVoter() {
        voterPlayer.setAlive(false);
        when(votingSessionRepository.findById(session.getId())).thenReturn(Optional.of(session));
        when(gamePlayerRepository.findByGameAndUser_Id(game, voter.getId())).thenReturn(Optional.of(voterPlayer));

        CastVoteResponse response = votingSessionService.castVote(session.getId(), voter.getId(), target.getId());

        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getMessage()).contains("Dead players");
    }

    @Test
    void castVoteRejectsDuplicateVote() {
        when(votingSessionRepository.findById(session.getId())).thenReturn(Optional.of(session));
        when(gamePlayerRepository.findByGameAndUser_Id(game, voter.getId())).thenReturn(Optional.of(voterPlayer));
        when(gamePlayerRepository.findByGameAndUser_Id(game, target.getId())).thenReturn(Optional.of(targetPlayer));
        when(gameVoteRepository.findByVotingSessionAndVoterId(session, voter.getId()))
                .thenReturn(Optional.of(new GameVote()));

        CastVoteResponse response = votingSessionService.castVote(session.getId(), voter.getId(), target.getId());

        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getMessage()).contains("already voted");
    }

    @Test
    void castVoteMarksCitizenNightVoteAsInvalid() {
        session.setPhase(GamePhase.NIGHT_VOTE);
        session.setTotalEligibleVoters(1);
        when(votingSessionRepository.findById(session.getId())).thenReturn(Optional.of(session));
        when(gamePlayerRepository.findByGameAndUser_Id(game, voter.getId())).thenReturn(Optional.of(voterPlayer));
        when(gamePlayerRepository.findByGameAndUser_Id(game, target.getId())).thenReturn(Optional.of(targetPlayer));
        when(gameVoteRepository.findByVotingSessionAndVoterId(session, voter.getId())).thenReturn(Optional.empty());
        when(votingSessionRepository.save(session)).thenReturn(session);
        when(gameVoteRepository.countVotesByTarget(session)).thenReturn(List.of());

        votingSessionService.castVote(session.getId(), voter.getId(), target.getId());

        ArgumentCaptor<GameVote> voteCaptor = ArgumentCaptor.forClass(GameVote.class);
        verify(gameVoteRepository).save(voteCaptor.capture());
        assertThat(voteCaptor.getValue().isValid()).isFalse();
    }

    @Test
    void castVoteCompletesSessionWhenAllPlayersVoted() {
        session.setTotalEligibleVoters(1);
        session.setVotesReceived(0);
        when(votingSessionRepository.findById(session.getId())).thenReturn(Optional.of(session));
        when(gamePlayerRepository.findByGameAndUser_Id(game, voter.getId())).thenReturn(Optional.of(voterPlayer));
        when(gamePlayerRepository.findByGameAndUser_Id(game, target.getId())).thenReturn(Optional.of(targetPlayer));
        when(gameVoteRepository.findByVotingSessionAndVoterId(session, voter.getId())).thenReturn(Optional.empty());
        when(votingSessionRepository.save(session)).thenReturn(session);
        when(gameVoteRepository.countVotesByTarget(session))
                .thenReturn(List.<Object[]>of(new Object[] {target.getId(), 1}));
        when(userRepository.findById(target.getId())).thenReturn(Optional.of(target));
        when(gamePlayerRepository.findByGameAndUser_Id(game, target.getId())).thenReturn(Optional.of(targetPlayer));

        CastVoteResponse response = votingSessionService.castVote(session.getId(), voter.getId(), target.getId());

        assertThat(response.isSuccess()).isTrue();
        verify(gameOrchestrator).handleVotingCompleted(eq(session), any(VotingResult.class));
    }

    @Test
    void expireSessionWithNoVotesTriggersNoElimination() {
        when(gameVoteRepository.countByVotingSession(session)).thenReturn(0L);

        votingSessionService.expireSession(session);

        assertThat(session.getStatus()).isEqualTo(VotingStatus.EXPIRED);
        verify(gameOrchestrator).handleVotingCompleted(eq(session), any(VotingResult.class));
    }

    @Test
    void expireSessionWithDayVoteTieDoesNotEliminate() {
        User a = GameTestFixtures.user("a");
        User b = GameTestFixtures.user("b");
        when(gameVoteRepository.countByVotingSession(session)).thenReturn(2L);
        when(gameVoteRepository.countVotesByTarget(session)).thenReturn(List.<Object[]>of(
                new Object[] {a.getId(), 2},
                new Object[] {b.getId(), 2}
        ));

        votingSessionService.expireSession(session);

        assertThat(session.isTie()).isTrue();
        verify(gameOrchestrator).handleVotingCompleted(eq(session), any(VotingResult.class));
        verify(gamePlayerRepository, never()).save(any());
    }

    @Test
    void castVoteRejectsExpiredSession() {
        session.setEndsAt(LocalDateTime.now().minusSeconds(5));
        when(votingSessionRepository.findById(session.getId())).thenReturn(Optional.of(session));
        when(gameVoteRepository.countByVotingSession(session)).thenReturn(0L);

        CastVoteResponse response = votingSessionService.castVote(session.getId(), voter.getId(), target.getId());

        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getMessage()).contains("expired");
    }
}
