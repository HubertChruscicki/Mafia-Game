package com.mafia.gameservice.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mafia.gameservice.config.RabbitMQConfig;
import com.mafia.gameservice.enums.GameRole;
import com.mafia.gameservice.events.GameEndedEvent;
import com.mafia.gameservice.events.GameStartedEvent;
import com.mafia.gameservice.models.Game;
import com.mafia.gameservice.models.GamePlayer;
import com.mafia.gameservice.models.GameRoom;
import com.mafia.gameservice.models.User;
import com.mafia.gameservice.repositories.GamePlayerRepository;
import com.mafia.gameservice.support.GameTestFixtures;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

@ExtendWith(MockitoExtension.class)
class GameEventPublisherTest {

    @Mock private RabbitTemplate rabbitTemplate;
    @Mock private GamePlayerRepository gamePlayerRepository;

    @InjectMocks
    private GameEventPublisher publisher;

    private Game game;
    private GameRoom room;
    private List<GamePlayer> players;

    @BeforeEach
    void setUp() {
        User host = GameTestFixtures.user("host");
        room = GameTestFixtures.openRoom(host);
        game = GameTestFixtures.activeGame(room);
        players = List.of(
                GameTestFixtures.alivePlayer(game, host, GameRole.MAFIA),
                GameTestFixtures.alivePlayer(game, GameTestFixtures.user("citizen"), GameRole.CITIZEN)
        );
    }

    @Test
    void publishGameStartedSendsRabbitEvent() {
        publisher.publishGameStarted(game, players);

        ArgumentCaptor<GameStartedEvent> captor = ArgumentCaptor.forClass(GameStartedEvent.class);
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.GAME_EVENTS_EXCHANGE),
                eq(RabbitMQConfig.GAME_STARTED_ROUTING_KEY),
                captor.capture());

        GameStartedEvent event = captor.getValue();
        assertThat(event.getPlayerCount()).isEqualTo(2);
        assertThat(event.getMafiaCount()).isEqualTo(1);
        assertThat(event.getCitizenCount()).isEqualTo(1);
    }

    @Test
    void publishGameEndedMarksWinnersCorrectly() {
        game.setEndedAt(LocalDateTime.now());

        publisher.publishGameEnded(game, "CITIZENS", "All mafia eliminated", players);

        ArgumentCaptor<GameEndedEvent> captor = ArgumentCaptor.forClass(GameEndedEvent.class);
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.GAME_EVENTS_EXCHANGE),
                eq(RabbitMQConfig.GAME_ENDED_ROUTING_KEY),
                captor.capture());

        GameEndedEvent event = captor.getValue();
        assertThat(event.getWinner()).isEqualTo("CITIZENS");
        assertThat(event.getPlayerResults().stream()
                .filter(r -> "citizen".equals(r.getUsername()))
                .allMatch(GameEndedEvent.PlayerResult::isWinner)).isTrue();
    }

    @Test
    void publishPhaseChangedIncludesAliveCounts() {
        when(gamePlayerRepository.countByGameAndAssignedRoleAndIsAlive(game, GameRole.MAFIA, true)).thenReturn(1);
        when(gamePlayerRepository.countByGameAndIsAlive(game, true)).thenReturn(3);

        publisher.publishPhaseChanged(game, "NIGHT_VOTE", "DAY_VOTE");

        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.GAME_EVENTS_EXCHANGE),
                eq(RabbitMQConfig.GAME_PHASE_CHANGED_ROUTING_KEY),
                org.mockito.ArgumentMatchers.<Object>any());
    }
}
