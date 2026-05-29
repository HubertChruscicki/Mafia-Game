package com.mafia.gameservice.controllers;

import com.mafia.gameservice.dto.*;
import com.mafia.gameservice.models.User;
import com.mafia.gameservice.services.GameService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/games")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
@Slf4j
public class GameController {

    private final GameService gameService;

    @PostMapping("/start")
    public ResponseEntity<GameStateResponse> start(@Valid @RequestBody StartGameRequest request) {
        return ResponseEntity.ok(gameService.startGame(request));
    }

    @GetMapping("/{gameId}")
    public ResponseEntity<GameStateResponse> get(@PathVariable UUID gameId) {
        return ResponseEntity.ok(gameService.getState(gameId));
    }

    @PostMapping("/{gameId}/advance-phase")
    public ResponseEntity<GameStateResponse> advancePhase(@PathVariable UUID gameId) {
        return ResponseEntity.ok(gameService.advancePhase(gameId));
    }

    @PostMapping("/{gameId}/end")
    public ResponseEntity<GameStateResponse> end(@PathVariable UUID gameId) {
        return ResponseEntity.ok(gameService.endGame(gameId));
    }

    @GetMapping("/rooms/{roomCode}/active-game")
    public ResponseEntity<GameWithPlayersDto> getActiveGame(@PathVariable String roomCode) {
        User user = getCurrentUser();
        return ResponseEntity.ok(gameService.getActiveGameByRoomCode(roomCode, user.getId()));
    }

    @GetMapping("/rooms/{roomCode}/me/role")
    public ResponseEntity<PlayerRoleDto> getMyRole(@PathVariable String roomCode) {
        User user = getCurrentUser();
        return ResponseEntity.ok(gameService.getPlayerRole(roomCode, user.getId()));
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (User) auth.getPrincipal();
    }
}
