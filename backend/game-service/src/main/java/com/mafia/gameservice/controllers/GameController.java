package com.mafia.gameservice.controllers;

import com.mafia.gameservice.models.User;
import com.mafia.gameservice.dto.GameStateResponse;
import com.mafia.gameservice.dto.GameWithPlayersDto;
import com.mafia.gameservice.dto.PlayerRoleDto;
import com.mafia.gameservice.dto.StartGameRequest;
import com.mafia.gameservice.services.GameService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/games")
@Tag(name = "Games", description = "Game lifecycle endpoints")
@RequiredArgsConstructor
public class GameController {

  private final GameService gameService;

  @PostMapping("/start")
  @PreAuthorize("isAuthenticated()")
  @Operation(summary = "Start a new game in a room")
  @ApiResponse(
      responseCode = "200",
      description = "Game started",
      content = @Content(mediaType = "application/json", schema = @Schema(implementation = GameStateResponse.class)))
  public ResponseEntity<GameStateResponse> start(@Valid @RequestBody StartGameRequest request) {
    return ResponseEntity.ok(gameService.startGame(request));
  }

  @GetMapping("/{gameId}")
  @PreAuthorize("isAuthenticated()")
  @Operation(summary = "Get current game state")
  public ResponseEntity<GameStateResponse> get(@PathVariable UUID gameId) {
    return ResponseEntity.ok(gameService.getState(gameId));
  }

  @PostMapping("/{gameId}/advance-phase")
  @PreAuthorize("isAuthenticated()")
  @Operation(summary = "Advance game phase")
  public ResponseEntity<GameStateResponse> advance(@PathVariable UUID gameId) {
    return ResponseEntity.ok(gameService.advancePhase(gameId));
  }

  @PostMapping("/{gameId}/end")
  @PreAuthorize("isAuthenticated()")
  @Operation(summary = "End game")
  public ResponseEntity<GameStateResponse> end(@PathVariable UUID gameId) {
    return ResponseEntity.ok(gameService.endGame(gameId));
  }

  @GetMapping("/rooms/{roomCode}/active-game")
  @PreAuthorize("isAuthenticated()")
  @Operation(summary = "Get active game for room with players")
  public ResponseEntity<GameWithPlayersDto> getActiveGame(@PathVariable String roomCode) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    User currentUser = (User) authentication.getPrincipal();

    GameWithPlayersDto game = gameService.getActiveGameByRoomCode(roomCode, currentUser.getId());
    return ResponseEntity.ok(game);
  }

  @GetMapping("/rooms/{roomCode}/me/role")
  @PreAuthorize("isAuthenticated()")
  @Operation(summary = "Get current player's role in the game")
  public ResponseEntity<PlayerRoleDto> getMyRole(@PathVariable String roomCode) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    User currentUser = (User) authentication.getPrincipal();

    PlayerRoleDto role = gameService.getPlayerRole(roomCode, currentUser.getId());
    return ResponseEntity.ok(role);
  }
}
