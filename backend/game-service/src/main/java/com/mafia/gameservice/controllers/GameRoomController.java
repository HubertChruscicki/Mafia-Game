package com.mafia.gameservice.controllers;

import com.mafia.gameservice.dto.gameRoom.*;
import com.mafia.gameservice.services.GameRoomService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

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
    log.info("Received request to create room: {}", createGameRoomReq);
    try {
      CreateGameRoomResp createGameRoomResp = gameRoomService.createRoom(createGameRoomReq);
      log.info("Successfully created room: {}", createGameRoomResp.getRoomCode());
      return ResponseEntity.status(HttpStatus.CREATED).body(createGameRoomResp);
    } catch (Exception e) {
      log.error("Error creating room", e);
      throw e;
    }
  }

  @PostMapping("/join/{roomCode}")
  public ResponseEntity<JoinGameRoomResp> joinGameRoom(@PathVariable String roomCode) {
    JoinGameRoomReq joinGameRoomReq = new JoinGameRoomReq(roomCode);
    JoinGameRoomResp joinGameRoomResp = gameRoomService.joinRoom(joinGameRoomReq);
    return ResponseEntity.ok(joinGameRoomResp);
  }

  @GetMapping("/{roomCode}")
  public ResponseEntity<GameRoomInfoResp> getGameRoomDetails(@PathVariable String roomCode) {
    GameRoomInfoResp gameRoomInfoResp = gameRoomService.getGameRoomInfoByCode(roomCode);
    return ResponseEntity.ok(gameRoomInfoResp);
  }

  @PostMapping("/info")
  public ResponseEntity<GameRoomListResp> getGameRoomsByFilter(
      @Valid @RequestBody GameRoomInfoReq gameRoomInfoReq) {
    log.info("Received request to /api/game_rooms/info with data: {}", gameRoomInfoReq);
    try {
      if (!gameRoomInfoReq.isValid()) {
        log.warn("Invalid GameRoomInfoReq: {}", gameRoomInfoReq);
        return ResponseEntity.badRequest().build();
      }
      log.debug("Calling gameRoomService.getGameRoomsByFilter");
      GameRoomListResp response = gameRoomService.getGameRoomsByFilter(gameRoomInfoReq);
      log.info("Successfully retrieved {} rooms", response.getRooms().size());
      return ResponseEntity.ok(response);
    } catch (Exception e) {
      log.error("Error in getGameRoomsByFilter with request: {}", gameRoomInfoReq, e);
      return ResponseEntity.internalServerError().build();
    }
  }

  @GetMapping("/search")
  public ResponseEntity<List<CreateGameRoomResp>> searchGameRooms(@RequestParam String name) {
    List<CreateGameRoomResp> gameRooms = gameRoomService.searchGameRoomsByName(name);
    return ResponseEntity.ok(gameRooms);
  }

  @PostMapping("/leave/{roomCode}")
  public ResponseEntity<LeaveGameRoomResp> leaveGameRoom(@PathVariable String roomCode) {
    LeaveGameRoomReq leaveGameRoomReq = new LeaveGameRoomReq(roomCode);
    var resp = gameRoomService.leaveRoom(leaveGameRoomReq);
    return ResponseEntity.ok(resp);
  }
}
