package com.mafia.gameservice.services;

import com.mafia.gameservice.dto.*;
import com.mafia.gameservice.enums.GamePhase;
import com.mafia.gameservice.enums.GameRole;
import com.mafia.gameservice.enums.GameRoomStatus;
import com.mafia.gameservice.enums.GameStatus;
import com.mafia.gameservice.models.*;
import com.mafia.gameservice.orchestration.GameOrchestrator;
import com.mafia.gameservice.repositories.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class GameService {

    private final GameRepository gameRepository;
    private final GameRoomRepository gameRoomRepository;
    private final GamePlayerRepository gamePlayerRepository;
    private final PlayerInRoomRepository playerInRoomRepository;
    private final VotingSessionService votingSessionService;
    private final SimpMessagingTemplate messagingTemplate;
    private final GameEventPublisher eventPublisher;
    private final GameOrchestrator gameOrchestrator;

    public GameService(
            GameRepository gameRepository,
            GameRoomRepository gameRoomRepository,
            GamePlayerRepository gamePlayerRepository,
            PlayerInRoomRepository playerInRoomRepository,
            VotingSessionService votingSessionService,
            SimpMessagingTemplate messagingTemplate,
            GameEventPublisher eventPublisher,
            @Lazy GameOrchestrator gameOrchestrator) {
        this.gameRepository = gameRepository;
        this.gameRoomRepository = gameRoomRepository;
        this.gamePlayerRepository = gamePlayerRepository;
        this.playerInRoomRepository = playerInRoomRepository;
        this.votingSessionService = votingSessionService;
        this.messagingTemplate = messagingTemplate;
        this.eventPublisher = eventPublisher;
        this.gameOrchestrator = gameOrchestrator;
    }

    @Transactional
    public GameStateResponse startGame(StartGameRequest request) {
        User currentUser = getCurrentUser();
        log.info("Starting game for room {} by user {}", request.getRoomCode(), currentUser.getUsername());

        GameRoom room = gameRoomRepository.findByRoomCode(request.getRoomCode())
                .orElseThrow(() -> new IllegalArgumentException("Room not found: " + request.getRoomCode()));

        // Validate host
        if (!room.getHost().getId().equals(currentUser.getId())) {
            throw new IllegalStateException("Only the host can start the game");
        }

        // Validate room is open
        if (room.getGameRoomStatus() != GameRoomStatus.OPEN) {
            throw new IllegalStateException("Room is not open: " + room.getGameRoomStatus());
        }

        // Check no active game already exists
        List<Game> activeGames = gameRepository.findByRoomAndStatus(room, GameStatus.IN_PROGRESS);
        if (!activeGames.isEmpty()) {
            throw new IllegalStateException("An active game already exists for this room");
        }

        // Get players in room
        List<PlayerInRoom> playersInRoom = playerInRoomRepository.findAllByGameRoom(room);
        int totalPlayers = playersInRoom.size();

        if (totalPlayers < 3) {
            throw new IllegalStateException("At least 3 players required to start game");
        }
        if (request.getMafiaCount() >= totalPlayers) {
            throw new IllegalStateException("Mafia count must be less than total players");
        }

        // Update room config and status
        room.setMafiaCount(request.getMafiaCount());
        room.setDiscussionTimeSeconds(request.getDiscussionTimeSeconds());
        room.setGameRoomStatus(GameRoomStatus.GAME_IN_PROGRESS);
        gameRoomRepository.save(room);
        broadcastRoomStatusUpdate(room);

        // Create game
        Game game = new Game();
        game.setRoom(room);
        game.setStatus(GameStatus.IN_PROGRESS);
        game.setCurrentPhase(GamePhase.NIGHT_VOTE);
        game.setCurrentDayNumber(1);
        game.setStartedAt(LocalDateTime.now());
        game = gameRepository.save(game);

        log.info("Game created: {}", game.getId());

        // Assign roles
        assignRolesToPlayers(game, playersInRoom, request.getMafiaCount());

        // Publish start event
        List<GamePlayer> gamePlayers = gamePlayerRepository.findAllByGameId(game.getId());
        eventPublisher.publishGameStarted(game, gamePlayers);

        // Start first voting session (NIGHT_VOTE)
        try {
            votingSessionService.startVotingSession(game, GamePhase.NIGHT_VOTE);
        } catch (Exception e) {
            log.error("Error starting first voting session", e);
            throw new IllegalStateException("Failed to start voting session: " + e.getMessage());
        }

        broadcastGameReady(room, game);
        broadcastPhaseChange(game);

        return toGameStateResponse(game);
    }

    @Transactional(readOnly = true)
    public GameStateResponse getState(UUID gameId) {
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new IllegalArgumentException("Game not found: " + gameId));
        return toGameStateResponse(game);
    }

    @Transactional
    public GameStateResponse advancePhase(UUID gameId) {
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new IllegalArgumentException("Game not found: " + gameId));
        gameOrchestrator.startNextPhase(game);
        return toGameStateResponse(game);
    }

    @Transactional
    public GameStateResponse endGame(UUID gameId) {
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new IllegalArgumentException("Game not found: " + gameId));
        GameOrchestrator.WinConditionResult result = gameOrchestrator.checkWinConditions(game);
        if (!result.hasWinner()) {
            // Force end
            game.setStatus(GameStatus.FINISHED);
            game.setEndedAt(LocalDateTime.now());
            GameRoom room = game.getRoom();
            room.setGameRoomStatus(GameRoomStatus.OPEN);
            gameRoomRepository.save(room);
            gameRepository.save(game);
        } else {
            gameOrchestrator.handleGameEnd(game, result);
        }
        return toGameStateResponse(game);
    }

    @Transactional(readOnly = true)
    public GameWithPlayersDto getActiveGameByRoomCode(String roomCode, UUID currentUserId) {
        GameRoom room = gameRoomRepository.findByRoomCode(roomCode)
                .orElseThrow(() -> new IllegalArgumentException("Room not found: " + roomCode));

        List<Game> active = gameRepository.findByRoomAndStatus(room, GameStatus.IN_PROGRESS);
        if (active.isEmpty()) {
            active = gameRepository.findByRoomAndStatus(room, GameStatus.FINISHED);
        }
        if (active.isEmpty()) {
            throw new NoSuchElementException("No game found for room: " + roomCode);
        }
        Game game = active.get(0);

        List<GamePlayer> gamePlayers = gamePlayerRepository.findByGame(game);
        boolean gameEnded = game.getStatus() == GameStatus.FINISHED;

        // Find the current user's own role
        String myRole = gamePlayers.stream()
                .filter(gp -> gp.getUser().getId().equals(currentUserId))
                .findFirst()
                .map(gp -> gp.getAssignedRole().name())
                .orElse(null);

        List<GamePlayerDto> playerDtos = gamePlayers.stream()
                .map(gp -> toGamePlayerDto(gp, currentUserId, gameEnded, room.getHost().getId()))
                .collect(Collectors.toList());

        return new GameWithPlayersDto(
                game.getId(),
                roomCode,
                game.getStatus().name(),
                game.getCurrentPhase().name(),
                myRole,
                game.getCurrentDayNumber(),
                playerDtos,
                game.getWinnerTeam()
        );
    }

    @Transactional(readOnly = true)
    public PlayerRoleDto getPlayerRole(String roomCode, UUID userId) {
        GameRoom room = gameRoomRepository.findByRoomCode(roomCode)
                .orElseThrow(() -> new IllegalArgumentException("Room not found: " + roomCode));

        List<Game> games = gameRepository.findByRoomAndStatus(room, GameStatus.IN_PROGRESS);
        if (games.isEmpty()) {
            games = gameRepository.findByRoomAndStatus(room, GameStatus.FINISHED);
        }
        if (games.isEmpty()) {
            throw new IllegalStateException("No game found for room: " + roomCode);
        }
        Game game = games.get(0);

        GamePlayer gp = gamePlayerRepository.findByGameAndUser_Id(game, userId)
                .orElseThrow(() -> new IllegalStateException("Player not found in game"));

        return new PlayerRoleDto(
                gp.getUser().getId(),
                gp.getUser().getUsername(),
                gp.getAssignedRole().name(),
                gp.isAlive()
        );
    }

    // ==================== PRIVATE HELPERS ====================

    private void assignRolesToPlayers(Game game, List<PlayerInRoom> playersInRoom, int mafiaCount) {
        List<PlayerInRoom> shuffled = new ArrayList<>(playersInRoom);
        Collections.shuffle(shuffled);

        for (int i = 0; i < shuffled.size(); i++) {
            PlayerInRoom pir = shuffled.get(i);
            GameRole role = i < mafiaCount ? GameRole.MAFIA : GameRole.CITIZEN;

            GamePlayer gp = new GamePlayer();
            gp.setGame(game);
            gp.setUser(pir.getUser());
            gp.setGameNick(pir.getUser().getUsername());
            gp.setAssignedRole(role);
            gp.setAlive(true);
            gamePlayerRepository.save(gp);

            log.info("Player {} assigned role {}", pir.getUser().getUsername(), role);
        }
    }

    private GameStateResponse toGameStateResponse(Game game) {
        List<GamePlayer> players = gamePlayerRepository.findByGame(game);
        UUID hostId = game.getRoom().getHost().getId();
        List<GamePlayerDto> playerDtos = players.stream()
                .map(gp -> toGamePlayerDto(gp, null, false, hostId))
                .collect(Collectors.toList());

        return new GameStateResponse(
                game.getId(),
                game.getRoom().getRoomCode(),
                game.getStatus().name(),
                game.getCurrentPhase().name(),
                game.getCurrentDayNumber(),
                playerDtos
        );
    }

    private GamePlayerDto toGamePlayerDto(GamePlayer gp, UUID currentUserId, boolean gameEnded, UUID hostId) {
        boolean isCurrentUser = currentUserId != null && gp.getUser().getId().equals(currentUserId);
        // Role is only visible to the owning player or after game ends
        String role = (isCurrentUser || gameEnded) ? gp.getAssignedRole().name() : null;
        boolean isHost = hostId != null && gp.getUser().getId().equals(hostId);
        return new GamePlayerDto(gp.getUser().getId(), gp.getUser().getUsername(), role, gp.isAlive(), isHost);
    }

    private void broadcastRoomStatusUpdate(GameRoom room) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("type", "room_status_changed");
            payload.put("roomCode", room.getRoomCode());
            payload.put("status", room.getGameRoomStatus().name());
            String topic = "/topic/game/" + room.getRoomCode() + "/updated";
            messagingTemplate.convertAndSend(topic, (Object) payload);
        } catch (Exception e) {
            log.error("Error broadcasting room status update", e);
        }
    }

    private void broadcastGameReady(GameRoom room, Game game) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("type", "game_ready");
            payload.put("gameId", game.getId());
            payload.put("roomCode", room.getRoomCode());
            payload.put("phase", game.getCurrentPhase().name());
            payload.put("dayNumber", game.getCurrentDayNumber());
            String topic = "/topic/game/" + room.getRoomCode() + "/ready";
            messagingTemplate.convertAndSend(topic, (Object) payload);
        } catch (Exception e) {
            log.error("Error broadcasting game ready", e);
        }
    }

    private void broadcastPhaseChange(Game game) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("type", "phase_change");
            payload.put("gameId", game.getId());
            payload.put("phase", game.getCurrentPhase().name());
            payload.put("dayNumber", game.getCurrentDayNumber());
            payload.put("isVotingPhase", true);
            String topic = "/topic/game/" + game.getRoom().getRoomCode() + "/phase/change";
            messagingTemplate.convertAndSend(topic, (Object) payload);
        } catch (Exception e) {
            log.error("Error broadcasting initial phase change", e);
        }
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new IllegalStateException("User is not authenticated");
        }
        return (User) auth.getPrincipal();
    }
}
