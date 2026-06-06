package com.mafia.gameservice.services;

import com.mafia.gameservice.config.RabbitMQConfig;
import com.mafia.gameservice.events.*;
import com.mafia.gameservice.models.Game;
import com.mafia.gameservice.models.GamePlayer;
import com.mafia.gameservice.models.GameRoom;
import com.mafia.gameservice.models.User;
import com.mafia.gameservice.repositories.GamePlayerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Serwis odpowiedzialny za publikację zdarzeń do RabbitMQ.
 *
 * Centralizuje logikę publikacji zdarzeń, zapewniając:
 * - Spójny format zdarzeń
 * - Obsługę błędów przy publikacji
 * - Logowanie wszystkich publikowanych zdarzeń
 *
 * Jest to kluczowy element architektury Event-Driven,
 * umożliwiający luźne powiązanie między komponentami systemu.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class GameEventPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final GamePlayerRepository gamePlayerRepository;

    /**
     * Publikuje zdarzenie rozpoczęcia gry.
     * Wywoływane gdy host uruchamia grę i role zostają przydzielone.
     */
    public void publishGameStarted(Game game, List<GamePlayer> players) {
        try {
            List<GameStartedEvent.PlayerInfo> playerInfos = players.stream()
                    .map(p -> GameStartedEvent.PlayerInfo.builder()
                            .userId(p.getUser().getId())
                            .username(p.getUser().getUsername())
                            .role(p.getAssignedRole().name())
                            .build())
                    .collect(Collectors.toList());

            long mafiaCount = players.stream()
                    .filter(p -> "MAFIA".equals(p.getAssignedRole().name()))
                    .count();

            GameStartedEvent event = GameStartedEvent.builder()
                    .gameId(game.getId())
                    .roomId(game.getRoom().getId())
                    .roomCode(game.getRoom().getRoomCode())
                    .roomName(game.getRoom().getName())
                    .playerCount(players.size())
                    .mafiaCount((int) mafiaCount)
                    .citizenCount(players.size() - (int) mafiaCount)
                    .players(playerInfos)
                    .startedAt(game.getStartedAt())
                    .build();

            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.GAME_EVENTS_EXCHANGE,
                    RabbitMQConfig.GAME_STARTED_ROUTING_KEY,
                    event
            );

            log.info("[RabbitMQ] Published GAME_STARTED: gameId={}, players={}, mafia={}",
                    game.getId(), players.size(), mafiaCount);

        } catch (Exception e) {
            log.error("[RabbitMQ] Failed to publish GAME_STARTED event", e);
        }
    }

    /**
     * Publikuje zdarzenie zmiany fazy gry.
     * Wywoływane przy każdym przejściu między fazami (noc→dzień, głosowanie→wyniki).
     */
    public void publishPhaseChanged(Game game, String previousPhase, String newPhase) {
        try {
            int aliveMafia = gamePlayerRepository.countByGameAndAssignedRoleAndIsAlive(
                    game, com.mafia.gameservice.enums.GameRole.MAFIA, true);
            int totalAlive = gamePlayerRepository.countByGameAndIsAlive(game, true);

            GamePhaseChangedEvent event = GamePhaseChangedEvent.builder()
                    .gameId(game.getId())
                    .roomCode(game.getRoom().getRoomCode())
                    .previousPhase(previousPhase)
                    .newPhase(newPhase)
                    .dayNumber(game.getCurrentDayNumber())
                    .alivePlayersCount(totalAlive)
                    .aliveMafiaCount(aliveMafia)
                    .aliveCitizensCount(totalAlive - aliveMafia)
                    .changedAt(LocalDateTime.now())
                    .build();

            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.GAME_EVENTS_EXCHANGE,
                    RabbitMQConfig.GAME_PHASE_CHANGED_ROUTING_KEY,
                    event
            );

            log.info("[RabbitMQ] Published PHASE_CHANGED: gameId={}, {} -> {}",
                    game.getId(), previousPhase, newPhase);

        } catch (Exception e) {
            log.error("[RabbitMQ] Failed to publish PHASE_CHANGED event", e);
        }
    }

    /**
     * Publikuje zdarzenie eliminacji gracza.
     * Wywoływane gdy gracz zostaje wyeliminowany w wyniku głosowania.
     */
    public void publishPlayerEliminated(Game game, User eliminatedUser,
                                        String role, String phase, int votesReceived) {
        try {
            int aliveMafia = gamePlayerRepository.countByGameAndAssignedRoleAndIsAlive(
                    game, com.mafia.gameservice.enums.GameRole.MAFIA, true);
            int totalAlive = gamePlayerRepository.countByGameAndIsAlive(game, true);

            PlayerEliminatedEvent event = PlayerEliminatedEvent.builder()
                    .gameId(game.getId())
                    .roomCode(game.getRoom().getRoomCode())
                    .eliminatedUserId(eliminatedUser.getId())
                    .eliminatedUsername(eliminatedUser.getUsername())
                    .eliminatedRole(role)
                    .eliminationPhase(phase)
                    .dayNumber(game.getCurrentDayNumber())
                    .votesReceived(votesReceived)
                    .remainingPlayers(totalAlive)
                    .remainingMafia(aliveMafia)
                    .remainingCitizens(totalAlive - aliveMafia)
                    .eliminatedAt(LocalDateTime.now())
                    .build();

            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.GAME_EVENTS_EXCHANGE,
                    RabbitMQConfig.GAME_PLAYER_ELIMINATED_ROUTING_KEY,
                    event
            );

            log.info("[RabbitMQ] Published PLAYER_ELIMINATED: gameId={}, user={}, role={}",
                    game.getId(), eliminatedUser.getUsername(), role);

        } catch (Exception e) {
            log.error("[RabbitMQ] Failed to publish PLAYER_ELIMINATED event", e);
        }
    }

    /**
     * Publikuje zdarzenie oddania głosu.
     * Wywoływane przy każdym oddanym głosie.
     */
    public void publishVoteCast(Game game, User voter, String voterRole,
                                User target, String phase, int currentVotes, int totalVoters) {
        try {
            VoteCastEvent event = VoteCastEvent.builder()
                    .gameId(game.getId())
                    .roomCode(game.getRoom().getRoomCode())
                    .voterId(voter.getId())
                    .voterUsername(voter.getUsername())
                    .voterRole(voterRole)
                    .targetId(target.getId())
                    .targetUsername(target.getUsername())
                    .votingPhase(phase)
                    .dayNumber(game.getCurrentDayNumber())
                    .currentVoteCount(currentVotes)
                    .totalEligibleVoters(totalVoters)
                    .votedAt(LocalDateTime.now())
                    .build();

            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.GAME_EVENTS_EXCHANGE,
                    RabbitMQConfig.GAME_VOTE_CAST_ROUTING_KEY,
                    event
            );

            log.debug("[RabbitMQ] Published VOTE_CAST: gameId={}, voter={} -> target={}",
                    game.getId(), voter.getUsername(), target.getUsername());

        } catch (Exception e) {
            log.error("[RabbitMQ] Failed to publish VOTE_CAST event", e);
        }
    }

    /**
     * Publikuje zdarzenie zakończenia gry.
     * Wywoływane gdy zostanie spełniony warunek wygranej.
     */
    public void publishGameEnded(Game game, String winner, String winReason,
                                 List<GamePlayer> players) {
        try {
            int durationSeconds = 0;
            if (game.getStartedAt() != null && game.getEndedAt() != null) {
                durationSeconds = (int) ChronoUnit.SECONDS.between(
                        game.getStartedAt(), game.getEndedAt());
            }

            List<GameEndedEvent.PlayerResult> results = players.stream()
                    .map(p -> GameEndedEvent.PlayerResult.builder()
                            .userId(p.getUser().getId())
                            .username(p.getUser().getUsername())
                            .role(p.getAssignedRole().name())
                            .survived(p.isAlive())
                            .eliminatedOnDay(p.isAlive() ? 0 : game.getCurrentDayNumber())
                            .isWinner(isWinner(p, winner))
                            .build())
                    .collect(Collectors.toList());

            GameEndedEvent event = GameEndedEvent.builder()
                    .gameId(game.getId())
                    .roomCode(game.getRoom().getRoomCode())
                    .roomName(game.getRoom().getName())
                    .winner(winner)
                    .winReason(winReason)
                    .totalDays(game.getCurrentDayNumber())
                    .durationSeconds(durationSeconds)
                    .playerResults(results)
                    .startedAt(game.getStartedAt())
                    .endedAt(game.getEndedAt())
                    .build();

            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.GAME_EVENTS_EXCHANGE,
                    RabbitMQConfig.GAME_ENDED_ROUTING_KEY,
                    event
            );

            log.info("[RabbitMQ] Published GAME_ENDED: gameId={}, winner={}, duration={}s",
                    game.getId(), winner, durationSeconds);

        } catch (Exception e) {
            log.error("[RabbitMQ] Failed to publish GAME_ENDED event", e);
        }
    }

    /**
     * Publikuje zdarzenie dołączenia gracza do pokoju.
     */
    public void publishPlayerJoinedRoom(GameRoom room, User player, int currentPlayerCount) {
        try {
            PlayerJoinedRoomEvent event = PlayerJoinedRoomEvent.builder()
                    .roomId(room.getId())
                    .roomCode(room.getRoomCode())
                    .roomName(room.getName())
                    .playerId(player.getId())
                    .playerUsername(player.getUsername())
                    .currentPlayerCount(currentPlayerCount)
                    .maxPlayers(room.getMaxPlayers())
                    .joinedAt(LocalDateTime.now())
                    .build();

            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.ROOM_EVENTS_EXCHANGE,
                    RabbitMQConfig.ROOM_PLAYER_JOINED_ROUTING_KEY,
                    event
            );

            log.info("[RabbitMQ] Published PLAYER_JOINED_ROOM: room={}, player={}, count={}/{}",
                    room.getRoomCode(), player.getUsername(), currentPlayerCount, room.getMaxPlayers());

        } catch (Exception e) {
            log.error("[RabbitMQ] Failed to publish PLAYER_JOINED_ROOM event", e);
        }
    }

    private boolean isWinner(GamePlayer player, String winner) {
        if ("MAFIA".equals(winner)) {
            return "MAFIA".equals(player.getAssignedRole().name());
        } else if ("CITIZENS".equals(winner)) {
            return "CITIZEN".equals(player.getAssignedRole().name());
        }
        return false;
    }
}
