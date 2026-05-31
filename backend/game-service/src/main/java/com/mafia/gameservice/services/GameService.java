package com.mafia.gameservice.services;

import com.mafia.gameservice.models.*;
import com.mafia.gameservice.dto.GameStateResponse;
import com.mafia.gameservice.dto.PlayerRoleDto;
import com.mafia.gameservice.dto.StartGameRequest;
import com.mafia.gameservice.enums.GamePhase;
import com.mafia.gameservice.enums.GameRole;
import com.mafia.gameservice.enums.GameRoomStatus;
import com.mafia.gameservice.enums.GameStatus;
import com.mafia.gameservice.exceptions.GameNotFoundException;
import com.mafia.gameservice.exceptions.GameRoomNotFoundException;
import com.mafia.gameservice.repositories.*;
import com.mafia.gameservice.services.voting.VotingSessionService;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class GameService {
  private final GameRepository gameRepository;
  private final GameRoomRepository gameRoomRepository;
  private final GamePlayerRepository gamePlayerRepository;
  private final PlayerInRoomRepository playerInRoomRepository;
  private final VotingSessionService votingSessionService;
  private final org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate;
  private final GameEventPublisher eventPublisher;

  @Transactional
  public GameStateResponse startGame(StartGameRequest request) {
    log.info(
        "Starting game for room {} with {} mafia and {}s discussion time",
        request.getRoomId(),
        request.getMafiaCount(),
        request.getDiscussionTimeSeconds());

    GameRoom room =
        gameRoomRepository
            .findById(request.getRoomId())
            .orElseThrow(() -> new GameRoomNotFoundException("Room not found"));

    // Ensure no active game exists for this room
    List<Game> active = gameRepository.findByRoom_IdAndStatus(room.getId(), GameStatus.IN_PROGRESS);
    if (!active.isEmpty()) {
      log.warn("Active game already exists for room {}", room.getId());
      throw new IllegalStateException("An active game already exists for this room");
    }

    // Pobierz graczy z pokoju
    List<PlayerInRoom> playersInRoom = playerInRoomRepository.findAllByGameRoom(room);
    if (playersInRoom.isEmpty()) {
      throw new IllegalStateException("No players in room");
    }

    int totalPlayers = playersInRoom.size();
    int mafiaCount = request.getMafiaCount();

    // Walidacja liczby graczy
    if (totalPlayers < 3) {
      throw new IllegalStateException("At least 3 players required to start game");
    }

    if (mafiaCount >= totalPlayers) {
      throw new IllegalStateException(
          "Mafia count must be less than total players (mafia: "
              + mafiaCount
              + ", total: "
              + totalPlayers
              + ")");
    }

    // Zapisz konfigurację w pokoju
    room.setMafiaCount(mafiaCount);
    room.setDiscussionTimeSeconds(request.getDiscussionTimeSeconds());
    room.setGameRoomStatus(GameRoomStatus.GAME_IN_PROGRESS);
    GameRoom savedRoom = gameRoomRepository.save(room);

    // Broadcast zmiany statusu pokoju przez WebSocket
    broadcastRoomStatusUpdate(savedRoom);

    // Utwórz grę
    Game game = new Game();
    game.setRoom(room);
    game.setStatus(GameStatus.IN_PROGRESS);
    game.setCurrentPhase(GamePhase.NIGHT_VOTE); // Zaczynamy od nocy
    game.setCurrentDayNumber(1);
    game.setStartedAt(LocalDateTime.now());

    Game savedGame = gameRepository.save(game);
    log.info("Game created: {}", savedGame.getId());

    // Przydziel role graczom
    assignRolesToPlayers(savedGame, playersInRoom, mafiaCount);
    
    // Pobierz graczy i opublikuj zdarzenie do RabbitMQ
    List<GamePlayer> gamePlayers = gamePlayerRepository.findAllByGameId(savedGame.getId());
    eventPublisher.publishGameStarted(savedGame, gamePlayers);

    // Rozpocznij pierwszą sesję głosowania (NIGHT_VOTE)
    try {
      votingSessionService.startVotingSession(
          savedGame, GamePhase.NIGHT_VOTE, request.getDiscussionTimeSeconds());
      log.info("First voting session (NIGHT_VOTE) started for game {}", savedGame.getId());
    } catch (Exception e) {
      log.error("Error starting voting session", e);
      throw new IllegalStateException("Failed to start voting session: " + e.getMessage());
    }

    // Broadcast "game_ready" - gra jest w pełni gotowa
    broadcastGameReady(savedRoom, savedGame);

    return toResponse(savedGame);
  }

  /**
   * Przydziela role graczom losowo
   */
  private void assignRolesToPlayers(Game game, List<PlayerInRoom> playersInRoom, int mafiaCount) {
    log.info("Assigning roles: {} mafia, {} citizens", mafiaCount, playersInRoom.size() - mafiaCount);

    // Losowo wybierz graczy
    List<PlayerInRoom> shuffledPlayers = new ArrayList<>(playersInRoom);
    Collections.shuffle(shuffledPlayers);

    // Przydziel role
    for (int i = 0; i < shuffledPlayers.size(); i++) {
      PlayerInRoom playerInRoom = shuffledPlayers.get(i);
      GameRole role = i < mafiaCount ? GameRole.MAFIA : GameRole.CITIZEN;

      GamePlayer gamePlayer = new GamePlayer();
      gamePlayer.setGame(game);
      gamePlayer.setUser(playerInRoom.getUser());
      gamePlayer.setGameNick(playerInRoom.getUser().getUsername());
      gamePlayer.setAssignedRole(role);
      gamePlayer.setAlive(true);

      gamePlayerRepository.save(gamePlayer);

      log.info(
          "Player {} assigned role: {}",
          playerInRoom.getUser().getUsername(),
          role);
    }
  }

  public GameStateResponse getState(UUID gameId) {
    Game game =
        gameRepository
            .findById(gameId)
            .orElseThrow(() -> new GameNotFoundException("Game not found"));
    return toResponse(game);
  }

  @Transactional
  public GameStateResponse advancePhase(UUID gameId) {
    Game game =
        gameRepository
            .findById(gameId)
            .orElseThrow(() -> new GameNotFoundException("Game not found"));

    // Simple phase switch logic
    if (game.getCurrentPhase() == GamePhase.DAY_VOTE) {
      game.setCurrentPhase(GamePhase.NIGHT_VOTE);
    } else {
      game.setCurrentPhase(GamePhase.DAY_VOTE);
      game.setCurrentDayNumber(game.getCurrentDayNumber() + 1);
    }

    Game saved = gameRepository.save(game);
    return toResponse(saved);
  }

  @Transactional
  public GameStateResponse endGame(UUID gameId) {
    Game game =
        gameRepository
            .findById(gameId)
            .orElseThrow(() -> new GameNotFoundException("Game not found"));

    game.setStatus(GameStatus.FINISHED);
    game.setEndedAt(LocalDateTime.now());

    Game saved = gameRepository.save(game);

    // Optionally update room status back
    GameRoom room = saved.getRoom();
    room.setGameRoomStatus(GameRoomStatus.OPEN);
    gameRoomRepository.save(room);

    return toResponse(saved);
  }

  /**
   * Sprawdza warunki wygranej po każdej eliminacji
   */
  @Transactional
  public void checkGameEndConditions(Game game) {
    log.info("Checking game end conditions for game {}", game.getId());

    // Policz żywych graczy według ról
    int aliveMafia =
        gamePlayerRepository.countByGameAndAssignedRoleAndIsAlive(game, GameRole.MAFIA, true);

    int totalAlive = gamePlayerRepository.countByGameAndIsAlive(game, true);
    int aliveCitizens = totalAlive - aliveMafia;

    log.info(
        "Game {} status: {} alive mafia, {} alive citizens",
        game.getId(),
        aliveMafia,
        aliveCitizens);

    // Warunek wygranej Obywateli: wszystkie mafie wyeliminowane
    if (aliveMafia == 0) {
      log.info("Citizens win! All mafia eliminated in game {}", game.getId());
      endGameWithWinner(game, "CITIZENS");
      return;
    }

    // Warunek wygranej Mafii: mafia >= obywatele
    if (aliveMafia >= aliveCitizens) {
      log.info("Mafia wins! Mafia count >= citizens in game {}", game.getId());
      endGameWithWinner(game, "MAFIA");
      return;
    }

    // Gra trwa dalej
    log.info("Game {} continues", game.getId());
  }

  /**
   * Kończy grę z określonym zwycięzcą
   */
  @Transactional
  public void endGameWithWinner(Game game, String winner) {
    log.info("Ending game {} with winner: {}", game.getId(), winner);

    game.setStatus(GameStatus.FINISHED);
    game.setCurrentPhase(GamePhase.GAME_OVER);
    game.setEndedAt(LocalDateTime.now());
    gameRepository.save(game);

    // Aktualizuj status pokoju
    GameRoom room = game.getRoom();
    room.setGameRoomStatus(GameRoomStatus.OPEN);
    gameRoomRepository.save(room);

    log.info("Game {} ended. Winner: {}", game.getId(), winner);
  }

  /**
   * Pobiera rolę gracza w aktywnej grze dla danego pokoju
   */
  @Transactional(readOnly = true)
  public PlayerRoleDto getPlayerRole(String roomCode, UUID userId) {
    log.info("Getting player role for user {} in room {}", userId, roomCode);

    GameRoom room =
        gameRoomRepository
            .findByRoomCode(roomCode)
            .orElseThrow(() -> new GameRoomNotFoundException("Room not found: " + roomCode));

    // Znajdź aktywną grę (lub ostatnią zakończoną)
    List<Game> games = gameRepository.findByRoom_IdAndStatus(room.getId(), GameStatus.IN_PROGRESS);
    
    if (games.isEmpty()) {
      // Sprawdź czy jest zakończona gra
      games = gameRepository.findByRoom_IdAndStatus(room.getId(), GameStatus.FINISHED);
      if (games.isEmpty()) {
        throw new IllegalStateException("No game found for room: " + roomCode);
      }
    }

    Game game = games.getFirst();
    
    // Znajdź gracza w grze
    GamePlayer gamePlayer = gamePlayerRepository.findByGameAndUser_Id(game, userId)
        .orElseThrow(() -> new IllegalStateException("Player not found in game"));

    PlayerRoleDto dto = new PlayerRoleDto();
    dto.setUserId(gamePlayer.getUser().getId());
    dto.setUsername(gamePlayer.getUser().getUsername());
    dto.setRole(gamePlayer.getAssignedRole());
    dto.setAlive(gamePlayer.isAlive());
    dto.setGameNick(gamePlayer.getGameNick());

    log.info("Player {} has role {} in room {}", userId, dto.getRole(), roomCode);
    return dto;
  }

  /**
   * Pobiera aktywną grę dla pokoju z listą graczy
   */
  @Transactional(readOnly = true)
  public com.mafia.gameservice.dto.GameWithPlayersDto getActiveGameByRoomCode(String roomCode, UUID currentUserId) {
    log.info("Getting active game for room: {}", roomCode);

    GameRoom room =
        gameRoomRepository
            .findByRoomCode(roomCode)
            .orElseThrow(() -> new GameRoomNotFoundException("Room not found: " + roomCode));

    // Znajdź aktywną grę
    List<Game> activeGames = gameRepository.findByRoom_IdAndStatus(room.getId(), GameStatus.IN_PROGRESS);
    
    if (activeGames.isEmpty()) {
      throw new GameNotFoundException("No active game found for room: " + roomCode);
    }

    Game game = activeGames.getFirst();
    
    // Pobierz graczy
    List<GamePlayer> gamePlayers = gamePlayerRepository.findByGame(game);
    boolean gameEnded = game.getStatus() == GameStatus.FINISHED;

    List<com.mafia.gameservice.dto.GamePlayerDto> playerDtos =
        gamePlayers.stream()
            .map(gp -> toGamePlayerDto(gp, currentUserId, gameEnded))
            .collect(Collectors.toList());

    com.mafia.gameservice.dto.GameWithPlayersDto dto = new com.mafia.gameservice.dto.GameWithPlayersDto();
    dto.setGameId(game.getId());
    dto.setRoomCode(roomCode);
    dto.setStatus(game.getStatus());
    dto.setCurrentPhase(game.getCurrentPhase());
    dto.setCurrentDayNumber(game.getCurrentDayNumber());
    dto.setPlayers(playerDtos);

    return dto;
  }

  /**
   * Konwertuje GamePlayer do DTO z uwzględnieniem widoczności roli
   */
  private com.mafia.gameservice.dto.GamePlayerDto toGamePlayerDto(GamePlayer gp, UUID currentUserId, boolean gameEnded) {
    com.mafia.gameservice.dto.GamePlayerDto dto = new com.mafia.gameservice.dto.GamePlayerDto();
    dto.setUserId(gp.getUser().getId());
    dto.setUsername(gp.getUser().getUsername());
    dto.setGameNick(gp.getGameNick());
    // Lombok @Data generates setters without the 'is' prefix: setAlive, setCurrentUser
    dto.setAlive(gp.isAlive());
    dto.setCurrentUser(gp.getUser().getId().equals(currentUserId));

    // Rola widoczna tylko dla właściciela lub po zakończeniu gry
    if (gp.getUser().getId().equals(currentUserId) || gameEnded) {
      dto.setAssignedRole(gp.getAssignedRole());
    } else {
      dto.setAssignedRole(null);
    }

    return dto;
  }

  private GameStateResponse toResponse(Game game) {
    return new GameStateResponse(
        game.getId(),
        game.getRoom().getId(),
        game.getStatus(),
        game.getCurrentPhase(),
        game.getCurrentDayNumber(),
        game.getCreatedAt(),
        game.getStartedAt(),
        game.getEndedAt());
  }

  /**
   * Broadcast zmiany statusu pokoju przez WebSocket
   * Używa tego samego formatu co GameRoomService dla spójności
   */
  private void broadcastRoomStatusUpdate(GameRoom room) {
    try {
      List<PlayerInRoom> playersInRoom = playerInRoomRepository.findAllByGameRoom(room);
      
      // Przygotuj listę graczy
      List<Map<String, Object>> players = playersInRoom.stream()
          .map(p -> {
            Map<String, Object> playerMap = new HashMap<>();
            playerMap.put("userId", p.getUser().getId());
            playerMap.put("username", p.getUser().getUsername());
            playerMap.put("isHost", p.getUser().getId().equals(room.getHost().getId()));
            playerMap.put("joinedAt", p.getJoinedAt());
            return playerMap;
          })
          .collect(Collectors.toList());
      
      // Przygotuj update w tym samym formacie co GameRoomService
      Map<String, Object> update = new HashMap<>();
      update.put("status", room.getGameRoomStatus());
      update.put("roomCode", room.getRoomCode());
      update.put("currentPlayers", playersInRoom.size());
      update.put("players", players);
      
      String topic = "/topic/game/" + room.getRoomCode() + "/updated";
      messagingTemplate.convertAndSend(topic, (Object) update);
      
      log.info("Broadcast room status update to topic: {} with status: {}", topic, room.getGameRoomStatus());
    } catch (Exception e) {
      log.error("Error broadcasting room status update for room: {}", room.getRoomCode(), e);
    }
  }

  /**
   * Broadcast "game_ready" - informuje klientów że gra jest w pełni gotowa
   * (role przydzielone, sesja głosowania uruchomiona)
   */
  private void broadcastGameReady(GameRoom room, Game game) {
    try {
      String roomCode = room.getRoomCode();
      
      Map<String, Object> payload = new HashMap<>();
      payload.put("type", "game_ready");
      payload.put("roomCode", roomCode);
      payload.put("gameId", game.getId());
      payload.put("phase", game.getCurrentPhase().name());
      payload.put("dayNumber", game.getCurrentDayNumber());
      payload.put("status", game.getStatus().name());
      
      String topic = "/topic/game/" + roomCode + "/ready";
      messagingTemplate.convertAndSend(topic, (Object) payload);
      
      log.info("Broadcast GAME_READY to topic: {} for game {}", topic, game.getId());
    } catch (Exception e) {
      log.warn("Failed to broadcast GAME_READY for room: {}", room.getRoomCode(), e);
    }
  }
}
