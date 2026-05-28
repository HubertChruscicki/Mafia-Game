package com.mafia.gameservice.services;

import com.mafia.gameservice.dto.PlayerInRoomResponse;
import com.mafia.gameservice.dto.gameroom.CreateGameRoomReq;
import com.mafia.gameservice.dto.gameroom.CreateGameRoomResp;
import com.mafia.gameservice.dto.gameroom.GameRoomInfoReq;
import com.mafia.gameservice.dto.gameroom.GameRoomInfoResp;
import com.mafia.gameservice.dto.gameroom.GameRoomListResp;
import com.mafia.gameservice.dto.gameroom.JoinGameRoomReq;
import com.mafia.gameservice.dto.gameroom.JoinGameRoomResp;
import com.mafia.gameservice.dto.gameroom.LeaveGameRoomReq;
import com.mafia.gameservice.dto.gameroom.LeaveGameRoomResp;
import com.mafia.gameservice.enums.GameRoomStatus;
import com.mafia.gameservice.models.GameRoom;
import com.mafia.gameservice.models.PlayerInRoom;
import com.mafia.gameservice.models.User;
import com.mafia.gameservice.repositories.GameRoomRepository;
import com.mafia.gameservice.repositories.PlayerInRoomRepository;
import com.mafia.gameservice.repositories.UserRepository;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    private final PlayerInRoomService playerInRoomService;

    private static final String ROOM_CODE_CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int ROOM_CODE_LENGTH = 6;
    private static final SecureRandom RANDOM = new SecureRandom();

    // -------------------------------------------------------------------------
    // Auth helpers
    // -------------------------------------------------------------------------

    private User requireAuthenticatedPrincipal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new IllegalStateException("User is not authenticated");
        }
        Object principal = auth.getPrincipal();
        if (!(principal instanceof User)) {
            throw new IllegalStateException("Principal is not a User instance");
        }
        return (User) principal;
    }

    private User loadUserFromDb(User principal) {
        return userRepository.findById(principal.getId())
                .orElseThrow(() -> new NoSuchElementException(
                        "User not found with ID: " + principal.getId()));
    }

    // -------------------------------------------------------------------------
    // Code generation
    // -------------------------------------------------------------------------

    private String generateUniqueRoomCode() {
        String code;
        do {
            StringBuilder sb = new StringBuilder(ROOM_CODE_LENGTH);
            for (int i = 0; i < ROOM_CODE_LENGTH; i++) {
                sb.append(ROOM_CODE_CHARACTERS.charAt(RANDOM.nextInt(ROOM_CODE_CHARACTERS.length())));
            }
            code = sb.toString();
        } while (gameRoomRepository.existsByRoomCode(code));
        return code;
    }

    // -------------------------------------------------------------------------
    // Response builders
    // -------------------------------------------------------------------------

    private List<PlayerInRoomResponse> buildPlayerList(GameRoom gameRoom,
                                                        List<PlayerInRoom> playersInRoom) {
        return playersInRoom.stream()
                .map(p -> {
                    boolean isHost = p.getUser().getId().equals(gameRoom.getHost().getId());
                    return new PlayerInRoomResponse(
                            p.getId(),
                            p.getUser().getId(),
                            p.getUser().getUsername(),
                            isHost,
                            p.getJoinedAt());
                })
                .collect(Collectors.toList());
    }

    private GameRoomInfoResp buildGameRoomInfoResp(GameRoom gameRoom) {
        List<PlayerInRoom> playersInRoom = playerInRoomRepository.findAllByGameRoom(gameRoom);
        List<PlayerInRoomResponse> players = buildPlayerList(gameRoom, playersInRoom);

        GameRoomInfoResp resp = new GameRoomInfoResp();
        resp.setId(gameRoom.getId());
        resp.setRoomCode(gameRoom.getRoomCode());
        resp.setName(gameRoom.getName());
        resp.setHostId(gameRoom.getHost().getId());
        resp.setHostUsername(gameRoom.getHost().getUsername());
        resp.setMaxPlayers(gameRoom.getMaxPlayers());
        resp.setCurrentPlayers(playersInRoom.size());
        resp.setStatus(gameRoom.getGameRoomStatus().name());
        resp.setMafiaCount(gameRoom.getMafiaCount());
        resp.setDiscussionTimeSeconds(gameRoom.getDiscussionTimeSeconds());
        resp.setCreatedAt(gameRoom.getCreatedAt());
        resp.setPlayers(players);
        return resp;
    }

    private Map<String, Object> buildUpdatePayload(GameRoom gameRoom) {
        List<PlayerInRoom> playersInRoom = playerInRoomRepository.findAllByGameRoom(gameRoom);
        List<PlayerInRoomResponse> players = buildPlayerList(gameRoom, playersInRoom);

        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "ROOM_UPDATED");
        payload.put("roomCode", gameRoom.getRoomCode());
        payload.put("currentPlayers", playersInRoom.size());
        payload.put("maxPlayers", gameRoom.getMaxPlayers());
        payload.put("status", gameRoom.getGameRoomStatus().name());
        payload.put("players", players);
        return payload;
    }

    private JoinGameRoomResp buildJoinResp(GameRoom gameRoom) {
        int currentPlayers = (int) playerInRoomRepository.countByGameRoom(gameRoom);
        return new JoinGameRoomResp(
                gameRoom.getRoomCode(),
                gameRoom.getName(),
                gameRoom.getHost().getUsername(),
                gameRoom.getMaxPlayers(),
                currentPlayers,
                gameRoom.getGameRoomStatus().name());
    }

    // -------------------------------------------------------------------------
    // Public service methods
    // -------------------------------------------------------------------------

    @Transactional
    public CreateGameRoomResp createRoom(CreateGameRoomReq req) {
        User host = loadUserFromDb(requireAuthenticatedPrincipal());

        GameRoom gameRoom = new GameRoom();
        gameRoom.setName(req.getName());
        gameRoom.setMaxPlayers(req.getMaxPlayers());
        gameRoom.setHost(host);
        gameRoom.setRoomCode(generateUniqueRoomCode());
        gameRoom.setGameRoomStatus(GameRoomStatus.OPEN);
        GameRoom saved = gameRoomRepository.save(gameRoom);

        log.info("Created game room '{}' with code: {}", saved.getName(), saved.getRoomCode());

        playerInRoomService.addPlayerToGameRoom(host, saved);

        return new CreateGameRoomResp(saved.getRoomCode(), saved.getName());
    }

    @Transactional
    public JoinGameRoomResp joinRoom(JoinGameRoomReq req) {
        User currentUser = loadUserFromDb(requireAuthenticatedPrincipal());

        GameRoom gameRoom = gameRoomRepository.findByRoomCode(req.getRoomCode())
                .orElseThrow(() -> new NoSuchElementException(
                        "Game room not found with code: " + req.getRoomCode()));

        if (gameRoom.getGameRoomStatus() != GameRoomStatus.OPEN) {
            throw new IllegalStateException("Cannot join a room that is not OPEN");
        }

        boolean alreadyIn = playerInRoomRepository.existsByGameRoomAndUser(gameRoom, currentUser);
        if (!alreadyIn) {
            long currentCount = playerInRoomRepository.countByGameRoom(gameRoom);
            if (currentCount >= gameRoom.getMaxPlayers()) {
                throw new IllegalStateException("Room is full");
            }
            playerInRoomService.addPlayerToGameRoom(currentUser, gameRoom);
            log.info("User '{}' joined room {}", currentUser.getUsername(), req.getRoomCode());
        }

        messagingTemplate.convertAndSend(
                "/topic/game/" + gameRoom.getRoomCode() + "/updated",
                (Object) buildUpdatePayload(gameRoom));

        return buildJoinResp(gameRoom);
    }

    @Transactional(readOnly = true)
    public GameRoomInfoResp getGameRoomInfoByCode(String roomCode) {
        GameRoom gameRoom = gameRoomRepository.findByRoomCode(roomCode)
                .orElseThrow(() -> new NoSuchElementException(
                        "Game room not found with code: " + roomCode));
        log.info("Fetching info for room: {}", roomCode);
        return buildGameRoomInfoResp(gameRoom);
    }

    @Transactional
    public LeaveGameRoomResp leaveRoom(LeaveGameRoomReq req) {
        User currentUser = loadUserFromDb(requireAuthenticatedPrincipal());

        GameRoom gameRoom = gameRoomRepository.findByRoomCode(req.getRoomCode())
                .orElseThrow(() -> new NoSuchElementException(
                        "Game room not found with code: " + req.getRoomCode()));

        List<PlayerInRoom> players = playerInRoomRepository.findAllByGameRoom(gameRoom);
        PlayerInRoom playerToRemove = players.stream()
                .filter(p -> p.getUser() != null && p.getUser().getId().equals(currentUser.getId()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("User is not in this room"));

        boolean wasHost = gameRoom.getHost().getId().equals(currentUser.getId());

        if (wasHost && gameRoom.getGameRoomStatus() == GameRoomStatus.OPEN) {
            gameRoomRepository.delete(gameRoom);
            messagingTemplate.convertAndSend(
                    "/topic/game/" + gameRoom.getRoomCode() + "/roomDeleted",
                    "Room " + gameRoom.getName() + " has been deleted by the host.");
            log.info("Host '{}' deleted room {}", currentUser.getUsername(), req.getRoomCode());
            return new LeaveGameRoomResp(true, "Room deleted by host");
        } else {
            playerInRoomRepository.delete(playerToRemove);
            log.info("User '{}' left room {}", currentUser.getUsername(), req.getRoomCode());
            messagingTemplate.convertAndSend(
                    "/topic/game/" + gameRoom.getRoomCode() + "/updated",
                    (Object) buildUpdatePayload(gameRoom));
            return new LeaveGameRoomResp(true, "Left room successfully");
        }
    }

    @Transactional(readOnly = true)
    public GameRoomListResp getGameRoomsByFilter(GameRoomInfoReq req) {
        if (req.isRoomCodeSearch()) {
            return GameRoomListResp.of(List.of(getGameRoomInfoByCode(req.getRoomCode())));
        } else if (req.isUserIdSearch()) {
            User user = userRepository.findById(req.getUserId())
                    .orElseThrow(() -> new NoSuchElementException(
                            "User not found with ID: " + req.getUserId()));
            List<GameRoomInfoResp> rooms = playerInRoomRepository.findAllByUser(user).stream()
                    .map(PlayerInRoom::getGameRoom)
                    .filter(Objects::nonNull)
                    .distinct()
                    .map(this::buildGameRoomInfoResp)
                    .collect(Collectors.toList());
            log.info("Found {} game rooms for user: {}", rooms.size(), user.getUsername());
            return GameRoomListResp.of(rooms);
        }
        log.warn("No filter provided in GameRoomInfoReq");
        return GameRoomListResp.of(List.of());
    }

    @Transactional(readOnly = true)
    public List<GameRoomInfoResp> searchGameRoomsByName(String name) {
        List<GameRoom> gameRooms = gameRoomRepository.findByNameContainingIgnoreCase(name);
        log.info("Found {} game rooms matching name: '{}'", gameRooms.size(), name);
        return gameRooms.stream()
                .map(this::buildGameRoomInfoResp)
                .collect(Collectors.toList());
    }
}
