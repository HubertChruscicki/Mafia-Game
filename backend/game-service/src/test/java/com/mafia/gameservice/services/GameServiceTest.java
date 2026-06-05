package com.mafia.gameservice.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mafia.gameservice.dto.StartGameRequest;
import com.mafia.gameservice.enums.GameRole;
import com.mafia.gameservice.enums.GameRoomStatus;
import com.mafia.gameservice.enums.GameStatus;
import com.mafia.gameservice.models.Game;
import com.mafia.gameservice.models.GamePlayer;
import com.mafia.gameservice.models.GameRoom;
import com.mafia.gameservice.models.PlayerInRoom;
import com.mafia.gameservice.models.User;
import com.mafia.gameservice.models.VotingSession;
import com.mafia.gameservice.orchestration.GameOrchestrator;
import com.mafia.gameservice.repositories.GamePlayerRepository;
import com.mafia.gameservice.repositories.GameRepository;
import com.mafia.gameservice.repositories.GameRoomRepository;
import com.mafia.gameservice.repositories.PlayerInRoomRepository;
import com.mafia.gameservice.support.GameTestFixtures;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class GameServiceTest {

    @Mock private GameRepository gameRepository;
    @Mock private GameRoomRepository gameRoomRepository;
    @Mock private GamePlayerRepository gamePlayerRepository;
    @Mock private PlayerInRoomRepository playerInRoomRepository;
    @Mock private VotingSessionService votingSessionService;
    @Mock private SimpMessagingTemplate messagingTemplate;
    @Mock private GameEventPublisher eventPublisher;
    @Mock private GameOrchestrator gameOrchestrator;

    @InjectMocks
    private GameService gameService;

    private User host;
    private GameRoom room;

    @BeforeEach
    void setUp() {
        host = GameTestFixtures.user("host");
        room = GameTestFixtures.openRoom(host);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(host, "", List.of()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void startGameRejectsNonHost() {
        User guest = GameTestFixtures.user("guest");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(guest, "", List.of()));
        when(gameRoomRepository.findByRoomCode("ABC123")).thenReturn(Optional.of(room));

        StartGameRequest request = new StartGameRequest("ABC123", 1, 60);

        assertThatThrownBy(() -> gameService.startGame(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Only the host");
    }

    @Test
    void startGameRejectsTooFewPlayers() {
        when(gameRoomRepository.findByRoomCode("ABC123")).thenReturn(Optional.of(room));
        when(gameRepository.findByRoomAndStatus(room, GameStatus.IN_PROGRESS)).thenReturn(List.of());
        when(playerInRoomRepository.findAllByGameRoom(room)).thenReturn(List.of(
                GameTestFixtures.playerInRoom(room, host)
        ));

        StartGameRequest request = new StartGameRequest("ABC123", 1, 60);

        assertThatThrownBy(() -> gameService.startGame(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("At least 3 players");
    }

    @Test
    void startGameCreatesGameAndAssignsRoles() {
        User p2 = GameTestFixtures.user("p2");
        User p3 = GameTestFixtures.user("p3");
        List<PlayerInRoom> players = List.of(
                GameTestFixtures.playerInRoom(room, host),
                GameTestFixtures.playerInRoom(room, p2),
                GameTestFixtures.playerInRoom(room, p3)
        );

        when(gameRoomRepository.findByRoomCode("ABC123")).thenReturn(Optional.of(room));
        when(gameRepository.findByRoomAndStatus(room, GameStatus.IN_PROGRESS)).thenReturn(List.of());
        when(playerInRoomRepository.findAllByGameRoom(room)).thenReturn(players);
        when(gameRepository.save(any(Game.class))).thenAnswer(inv -> {
            Game saved = inv.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });
        when(gamePlayerRepository.findAllByGameId(any())).thenReturn(List.of());
        when(votingSessionService.startVotingSession(any(), any())).thenReturn(new VotingSession());

        gameService.startGame(new StartGameRequest("ABC123", 1, 90));

        assertThat(room.getGameRoomStatus()).isEqualTo(GameRoomStatus.GAME_IN_PROGRESS);
        assertThat(room.getMafiaCount()).isEqualTo(1);
        verify(gamePlayerRepository, org.mockito.Mockito.times(3)).save(any(GamePlayer.class));
        verify(eventPublisher).publishGameStarted(any(Game.class), any());
        verify(votingSessionService).startVotingSession(any(Game.class), eq(com.mafia.gameservice.enums.GamePhase.NIGHT_VOTE));
    }

    @Test
    void getActiveGameByRoomCodeHidesRolesFromOtherPlayers() {
        Game game = GameTestFixtures.activeGame(room);
        User other = GameTestFixtures.user("other");
        GamePlayer hostPlayer = GameTestFixtures.alivePlayer(game, host, GameRole.MAFIA);
        GamePlayer otherPlayer = GameTestFixtures.alivePlayer(game, other, GameRole.CITIZEN);

        when(gameRoomRepository.findByRoomCode("ABC123")).thenReturn(Optional.of(room));
        when(gameRepository.findByRoomAndStatus(room, GameStatus.IN_PROGRESS)).thenReturn(List.of(game));
        when(gamePlayerRepository.findByGame(game)).thenReturn(List.of(hostPlayer, otherPlayer));

        var dto = gameService.getActiveGameByRoomCode("ABC123", other.getId());

        assertThat(dto.getMyRole()).isEqualTo("CITIZEN");
        assertThat(dto.getPlayers()).anyMatch(p ->
                p.getUserId().equals(host.getId()) && p.getRole() == null);
        assertThat(dto.getPlayers()).anyMatch(p ->
                p.getUserId().equals(other.getId()) && "CITIZEN".equals(p.getRole()));
    }

    @Test
    void getPlayerRoleReturnsOwnRole() {
        Game game = GameTestFixtures.activeGame(room);
        GamePlayer hostPlayer = GameTestFixtures.alivePlayer(game, host, GameRole.MAFIA);

        when(gameRoomRepository.findByRoomCode("ABC123")).thenReturn(Optional.of(room));
        when(gameRepository.findByRoomAndStatus(room, GameStatus.IN_PROGRESS)).thenReturn(List.of(game));
        when(gamePlayerRepository.findByGameAndUser_Id(game, host.getId())).thenReturn(Optional.of(hostPlayer));

        var roleDto = gameService.getPlayerRole("ABC123", host.getId());

        assertThat(roleDto.getRole()).isEqualTo("MAFIA");
        assertThat(roleDto.isAlive()).isTrue();
    }

    @Test
    void endGameUsesOrchestratorWhenWinnerExists() {
        Game game = GameTestFixtures.activeGame(room);
        when(gameRepository.findById(game.getId())).thenReturn(Optional.of(game));
        when(gameOrchestrator.checkWinConditions(game))
                .thenReturn(new GameOrchestrator.WinConditionResult(true, "CITIZENS", "done"));

        gameService.endGame(game.getId());

        verify(gameOrchestrator).handleGameEnd(game,
                new GameOrchestrator.WinConditionResult(true, "CITIZENS", "done"));
    }
}
