package com.mafia.gameservice.services;

import com.mafia.gameservice.config.RabbitMQConfig;
import com.mafia.gameservice.models.GameRoom;
import com.mafia.gameservice.models.PlayerInRoom;
import com.mafia.gameservice.models.User;
import com.mafia.gameservice.dto.gameRoom.*;
import com.mafia.gameservice.enums.GameRoomStatus;
import com.mafia.gameservice.exceptions.ForbiddenActionException;
import com.mafia.gameservice.exceptions.GameRoomNotFoundException;
import com.mafia.gameservice.exceptions.UserNotFoundException;
import com.mafia.gameservice.repositories.GameRoomRepository;
import com.mafia.gameservice.repositories.PlayerInRoomRepository;
import com.mafia.gameservice.repositories.UserRepository;
import java.security.SecureRandom;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class GameRoomService {

  private final GameRoomRepository gameRoomRepository;
  private final UserRepository userRepository;
  private final PlayerInRoomRepository playerInRoomRepository;
  private final SimpMessagingTemplate messagingTemplate;
  private final RabbitTemplate rabbitTemplate;
  private final PlayerInRoomService playerInRoomService;
  private final GameEventPublisher eventPublisher;

  private static final String ROOM_CODE_CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
  private static final int ROOM_CODE_LENGTH = 6;
  private static final SecureRandom random = new SecureRandom();

  private User requireAuthenticatedPrincipalUser() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !authentication.isAuthenticated()) {
      throw new IllegalStateException("User is not authenticated");
    }
    Object principal = authentication.getPrincipal();
    if (!(principal instanceof User)) {
      throw new IllegalStateException("Principal is not a User instance");
    }
    return (User) principal;
  }

  private User getPrincipalUserFromRepository(User principalUser) {
    return userRepository
        .findById(principalUser.getId())
        .orElseThrow(
            () -> new UserNotFoundException("User not found with ID: " + principalUser.getId()));
  }

  private String generateUniqueGameRoomCode() {
    StringBuilder sb;
    String code;
    do {
      sb = new StringBuilder(ROOM_CODE_LENGTH);
      for (int i = 0; i < ROOM_CODE_LENGTH; i++) {
        sb.append(ROOM_CODE_CHARACTERS.charAt(random.nextInt(ROOM_CODE_CHARACTERS.length())));
      }
      code = sb.toString();
    } while (gameRoomRepository.existsByRoomCode(code));
    return code;
  }

  private CreateGameRoomResp prepareCreateGameRoomResp(GameRoom gameRoom) {
    return new CreateGameRoomResp(gameRoom.getRoomCode(), gameRoom.getName());
  }

  private GameRoomInfoResp prepareGameRoomInfoResp(GameRoom gameRoom) {
    List<PlayerInRoom> playersInRoom = playerInRoomRepository.findAllByGameRoom(gameRoom);
    List<String> playerIds =
        playersInRoom.stream()
            .map(p -> p.getUser().getId().toString())
            .collect(Collectors.toList());

    List<com.mafia.gameservice.dto.PlayerInRoomResponse> players =
        playersInRoom.stream()
            .map(
                p -> {
                  boolean isHost = p.getUser().getId().equals(gameRoom.getHost().getId());
                  return new com.mafia.gameservice.dto.PlayerInRoomResponse(
                      p.getId(),
                      p.getUser().getId(),
                      p.getUser().getUsername(),
                      isHost,
                      p.getJoinedAt());
                })
            .collect(Collectors.toList());

    GameRoomInfoResp resp = new GameRoomInfoResp();
    resp.setId(gameRoom.getId()); // ✅ DODANE
    resp.setRoomCode(gameRoom.getRoomCode());
    resp.setName(gameRoom.getName());
    resp.setHostId(gameRoom.getHost().getId().toString());
    resp.setHostUsername(gameRoom.getHost().getUsername());
    resp.setMaxPlayers(gameRoom.getMaxPlayers());
    resp.setCurrentPlayers(playersInRoom.size());
    resp.setStatus(gameRoom.getGameRoomStatus());
    resp.setCreatedAt(gameRoom.getCreatedAt());
    resp.setPlayerIds(playerIds);
    resp.setPlayers(players);
    
    return resp;
  }

  private GameRoomUpdateInd prepareGameRoomUpdateInd(GameRoom gameRoom) {
    List<PlayerInRoom> playersInRoom = playerInRoomRepository.findAllByGameRoom(gameRoom);
    List<String> playerIds =
        playersInRoom.stream()
            .map(p -> p.getUser().getId().toString())
            .collect(Collectors.toList());

    List<com.mafia.gameservice.dto.PlayerInRoomResponse> players =
        playersInRoom.stream()
            .map(
                p -> {
                  boolean isHost = p.getUser().getId().equals(gameRoom.getHost().getId());
                  return new com.mafia.gameservice.dto.PlayerInRoomResponse(
                      p.getId(),
                      p.getUser().getId(),
                      p.getUser().getUsername(),
                      isHost,
                      p.getJoinedAt());
                })
            .collect(Collectors.toList());

    GameRoomUpdateInd update = new GameRoomUpdateInd();
    update.setRoomCode(gameRoom.getRoomCode());
    update.setCurrentPlayers(playersInRoom.size());
    update.setStatus(gameRoom.getGameRoomStatus());
    update.setPlayerIds(playerIds);
    update.setPlayers(players);
    
    return update;
  }

  private JoinGameRoomResp prepareJoinResp(GameRoom gameRoom) {
    int currentNumOfPlayers = playerInRoomRepository.findAllByGameRoom(gameRoom).size();
    return new JoinGameRoomResp(
        gameRoom.getRoomCode(),
        gameRoom.getName(),
        gameRoom.getHost().getUsername(),
        gameRoom.getMaxPlayers(),
        currentNumOfPlayers,
        gameRoom.getGameRoomStatus().name());
  }

  @Transactional(readOnly = true)
  public List<CreateGameRoomResp> searchGameRoomsByName(String name) {
    try {
      List<GameRoom> gameRooms = gameRoomRepository.findByNameContainingIgnoreCase(name);
      log.info("Found {} game rooms matching name: {}", gameRooms.size(), name);

      return gameRooms.stream().map(this::prepareCreateGameRoomResp).collect(Collectors.toList());
    } catch (Exception e) {
      log.error("Error searching game rooms by name: {}", e.toString());
      throw e;
    }
  }

  @Transactional
  public CreateGameRoomResp createRoom(CreateGameRoomReq createGameRoomReq) {
    try {
      var principalUser = requireAuthenticatedPrincipalUser();
      var host = getPrincipalUserFromRepository(principalUser);

      GameRoom gameRoom = new GameRoom();
      gameRoom.setName(createGameRoomReq.getName());
      gameRoom.setMaxPlayers(createGameRoomReq.getMaxPlayers());
      gameRoom.setHost(host);
      gameRoom.setRoomCode(generateUniqueGameRoomCode());
      gameRoom.setGameRoomStatus(GameRoomStatus.OPEN);
      GameRoom savedRoom = gameRoomRepository.save(gameRoom);
      log.info("Created game room: {} with code: {}", savedRoom.getId(), savedRoom.getRoomCode());

      playerInRoomService.addPlayerToGameRoom(host, gameRoom);

      CreateGameRoomResp createGameRoomResp = prepareCreateGameRoomResp(savedRoom);

      try {
        rabbitTemplate.convertAndSend(
            RabbitMQConfig.ROOM_EVENTS_EXCHANGE,
            RabbitMQConfig.ROOM_CREATED_ROUTING_KEY,
            createGameRoomResp);
        log.info("Sent room creation event to RabbitMQ: {}", createGameRoomResp.getRoomCode());
      } catch (Exception e) {
        log.error("Error sending room creation event to RabbitMQ", e);
      }

      return createGameRoomResp;
    } catch (Exception e) {
      log.error("Error creating game room", e);
      throw e;
    }
  }

  /**
   * Get single game room by room code
   *
   * @param roomCode the room code
   * @return GameRoomInfoResp with single room details
   */
  @Transactional(readOnly = true)
  public GameRoomInfoResp getGameRoomInfoByCode(String roomCode) {
    try {
      GameRoom gameRoom =
          gameRoomRepository
              .findByRoomCode(roomCode)
              .orElseThrow(
                  () ->
                      new GameRoomNotFoundException("Game room not found with code: " + roomCode));
      log.info("Found game room by code: {}", roomCode);
      return prepareGameRoomInfoResp(gameRoom);
    } catch (Exception e) {
      log.error("Error getting room details for: {}", roomCode, e);
      throw e;
    }
  }

  /**
   * Get game rooms by filter (either by room code or user ID) Frontend sends userId directly in
   * request - no backend authentication needed
   *
   * @param gameRoomInfoReq GameRoomInfoReq with either roomCode or userId
   * @return GameRoomListResp containing list of rooms
   */
  @Transactional(readOnly = true)
  public GameRoomListResp getGameRoomsByFilter(GameRoomInfoReq gameRoomInfoReq) {
    try {
      if (gameRoomInfoReq.isRoomCodeSearch()) {
        return GameRoomListResp.of(List.of(getGameRoomInfoByCode(gameRoomInfoReq.getRoomCode())));
      } else if (gameRoomInfoReq.isUserIdSearch()) {
        var user =
            userRepository
                .findById(gameRoomInfoReq.getUserId())
                .orElseThrow(
                    () ->
                        new UserNotFoundException(
                            "User not found with ID: " + gameRoomInfoReq.getUserId()));
        var memberships = playerInRoomRepository.findAllByUser(user);
        var rooms =
            memberships.stream()
                .map(PlayerInRoom::getGameRoom)
                .filter(Objects::nonNull)
                .distinct()
                .map(this::prepareGameRoomInfoResp)
                .collect(Collectors.toList());
        log.info("Found {} game rooms for user: {}", rooms.size(), user.getUsername());
        return GameRoomListResp.of(rooms);
      } else {
        // No filter provided
        log.warn("No filter provided in GameRoomInfoReq");
        return GameRoomListResp.of(List.of());
      }
    } catch (Exception e) {
      log.error("Error getting game room info: {}", e.getMessage(), e);
      throw e;
    }
  }

  @Transactional
  public JoinGameRoomResp joinRoom(JoinGameRoomReq req) {
    try {
      var principalUser = requireAuthenticatedPrincipalUser();
      var currentUser = getPrincipalUserFromRepository(principalUser);

      GameRoom gameRoom =
          gameRoomRepository
              .findByRoomCode(req.getRoomCode())
              .orElseThrow(
                  () ->
                      new GameRoomNotFoundException(
                          "Game room not found with code: " + req.getRoomCode()));

      if (gameRoom.getGameRoomStatus() != GameRoomStatus.OPEN) {
        throw new ForbiddenActionException("Cannot join a room that is not OPEN");
      }

      playerInRoomService.addPlayerToGameRoom(currentUser, gameRoom);

      // Publikuj zdarzenie dołączenia do RabbitMQ
      int currentPlayerCount = playerInRoomRepository.findAllByGameRoom(gameRoom).size();
      eventPublisher.publishPlayerJoinedRoom(gameRoom, currentUser, currentPlayerCount);

      var resp = prepareJoinResp(gameRoom);

      messagingTemplate.convertAndSend(
          "/topic/game/" + gameRoom.getRoomCode() + "/playerJoined", resp);
      messagingTemplate.convertAndSend(
          "/topic/game/" + gameRoom.getRoomCode() + "/updated", prepareGameRoomUpdateInd(gameRoom));

      return resp;
    } catch (Exception e) {
      log.error("Error joining room: {}", req.getRoomCode(), e);
      throw e;
    }
  }

  @Transactional
  public LeaveGameRoomResp leaveRoom(LeaveGameRoomReq req) {
    try {
      var principalUser = requireAuthenticatedPrincipalUser();
      var currentUser = getPrincipalUserFromRepository(principalUser);

      GameRoom gameRoom =
          gameRoomRepository
              .findByRoomCode(req.getRoomCode())
              .orElseThrow(
                  () ->
                      new GameRoomNotFoundException(
                          "Game room not found with code: " + req.getRoomCode()));

      var players = playerInRoomRepository.findAllByGameRoom(gameRoom);
      PlayerInRoom playerToRemove =
          players.stream()
              .filter(p -> p.getUser() != null && p.getUser().getId().equals(currentUser.getId()))
              .findFirst()
              .orElseThrow(() -> new UserNotFoundException("User is not in this room."));

      if (gameRoom.getGameRoomStatus() == GameRoomStatus.CLOSED) {
        return new LeaveGameRoomResp(req.getRoomCode(), false, players.size());
      }

      boolean wasHost = gameRoom.getHost().getId().equals(currentUser.getId());

      if (wasHost) {
        gameRoomRepository.delete(gameRoom);
        messagingTemplate.convertAndSend(
            "/topic/game/" + gameRoom.getRoomCode() + "/roomDeleted",
            "Room " + gameRoom.getName() + " has been deleted by the host.");
        log.info("Host {} deleted room {}", currentUser.getUsername(), req.getRoomCode());
        return new LeaveGameRoomResp(req.getRoomCode(), true, 0);
      } else {
        playerInRoomRepository.delete(playerToRemove);
        log.info("User {} left room {}", currentUser.getUsername(), req.getRoomCode());

        CreateGameRoomResp roomResponse = prepareCreateGameRoomResp(gameRoom);

        messagingTemplate.convertAndSend(
            "/topic/game/" + gameRoom.getRoomCode() + "/playerLeft", roomResponse);
        messagingTemplate.convertAndSend(
            "/topic/game/" + gameRoom.getRoomCode() + "/updated",
            prepareGameRoomUpdateInd(gameRoom));

        int remaining = playerInRoomRepository.findAllByGameRoom(gameRoom).size();
        return new LeaveGameRoomResp(req.getRoomCode(), false, remaining);
      }
    } catch (Exception e) {
      log.error("Error leaving room: {}", req.getRoomCode(), e);
      throw e;
    }
  }

  @Transactional
  public void endRoom(String roomCode) {
    try {
      Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
      User principalUser = (User) authentication.getPrincipal();
      User currentUser =
          userRepository
              .findById(principalUser.getId())
              .orElseThrow(
                  () ->
                      new UserNotFoundException(
                          "User not found with ID: " + principalUser.getId()));

      GameRoom gameRoom =
          gameRoomRepository
              .findByRoomCode(roomCode)
              .orElseThrow(
                  () ->
                      new GameRoomNotFoundException("Game room not found with code: " + roomCode));

      if (!gameRoom.getHost().getId().equals(currentUser.getId())) {
        throw new ForbiddenActionException("Only the host can end the room.");
      }

      if (gameRoom.getGameRoomStatus() == GameRoomStatus.CLOSED) {
        return;
      }

      gameRoom.setGameRoomStatus(GameRoomStatus.CLOSED);
      GameRoom updatedRoom = gameRoomRepository.save(gameRoom);
      // CreateGameRoomResp roomResponse = prepareCreateGameRoomResp(updatedRoom);

      messagingTemplate.convertAndSend(
          "/topic/game/" + updatedRoom.getRoomCode() + "/updated",
          prepareGameRoomUpdateInd(updatedRoom));

      log.info("Host {} ended room {}", currentUser.getUsername(), roomCode);
    } catch (Exception e) {
      log.error("Error ending room: {}", roomCode, e);
      throw e;
    }
  }
}
