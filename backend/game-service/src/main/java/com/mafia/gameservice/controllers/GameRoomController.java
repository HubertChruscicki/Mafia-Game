package com.mafia.gameservice.controllers;

import com.mafia.gameservice.dto.gameroom.CreateGameRoomReq;
import com.mafia.gameservice.dto.gameroom.CreateGameRoomResp;
import com.mafia.gameservice.dto.gameroom.GameRoomInfoReq;
import com.mafia.gameservice.dto.gameroom.GameRoomInfoResp;
import com.mafia.gameservice.dto.gameroom.GameRoomListResp;
import com.mafia.gameservice.dto.gameroom.JoinGameRoomReq;
import com.mafia.gameservice.dto.gameroom.JoinGameRoomResp;
import com.mafia.gameservice.dto.gameroom.LeaveGameRoomReq;
import com.mafia.gameservice.dto.gameroom.LeaveGameRoomResp;
import com.mafia.gameservice.services.GameRoomService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/game_rooms")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("isAuthenticated()")
public class GameRoomController {

    private final GameRoomService gameRoomService;

    @PostMapping("/create")
    public ResponseEntity<CreateGameRoomResp> createGameRoom(
            @Valid @RequestBody CreateGameRoomReq createGameRoomReq) {
        log.info("Received request to create room: {}", createGameRoomReq.getName());
        CreateGameRoomResp resp = gameRoomService.createRoom(createGameRoomReq);
        log.info("Successfully created room: {}", resp.getRoomCode());
        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }

    @PostMapping("/join/{roomCode}")
    public ResponseEntity<JoinGameRoomResp> joinGameRoom(@PathVariable String roomCode) {
        JoinGameRoomResp resp = gameRoomService.joinRoom(new JoinGameRoomReq(roomCode));
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/{roomCode}")
    public ResponseEntity<GameRoomInfoResp> getGameRoomDetails(@PathVariable String roomCode) {
        GameRoomInfoResp resp = gameRoomService.getGameRoomInfoByCode(roomCode);
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/info")
    public ResponseEntity<GameRoomListResp> getGameRoomsByFilter(
            @Valid @RequestBody GameRoomInfoReq gameRoomInfoReq) {
        log.info("Received request to /api/game_rooms/info: {}", gameRoomInfoReq);
        if (!gameRoomInfoReq.isValid()) {
            log.warn("Invalid GameRoomInfoReq received");
            return ResponseEntity.badRequest().build();
        }
        GameRoomListResp response = gameRoomService.getGameRoomsByFilter(gameRoomInfoReq);
        log.info("Returning {} rooms", response.getRooms().size());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/search")
    public ResponseEntity<List<GameRoomInfoResp>> searchGameRooms(@RequestParam String name) {
        List<GameRoomInfoResp> rooms = gameRoomService.searchGameRoomsByName(name);
        return ResponseEntity.ok(rooms);
    }

    @PostMapping("/leave/{roomCode}")
    public ResponseEntity<LeaveGameRoomResp> leaveGameRoom(@PathVariable String roomCode) {
        LeaveGameRoomResp resp = gameRoomService.leaveRoom(new LeaveGameRoomReq(roomCode));
        return ResponseEntity.ok(resp);
    }
}
