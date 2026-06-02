package com.mafia.gameservice.controllers;

import com.mafia.gameservice.models.Game;
import com.mafia.gameservice.models.User;
import com.mafia.gameservice.models.VotingSession;
import com.mafia.gameservice.dto.voting.CastVoteRequest;
import com.mafia.gameservice.dto.voting.CastVoteResponse;
import com.mafia.gameservice.dto.voting.VoteResultDto;
import com.mafia.gameservice.dto.voting.VotingSessionDto;
import com.mafia.gameservice.exceptions.GameNotFoundException;
import com.mafia.gameservice.repositories.GameRepository;
import com.mafia.gameservice.services.voting.VotingSessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/** Voting controller mapped into com.mafia.gameservice */
@RestController
@RequestMapping("/api/games/{gameId}/voting")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@Tag(name = "Voting", description = "Voting management endpoints")
@SecurityRequirement(name = "bearerAuth")
@Slf4j
public class VotingController {

  private final VotingSessionService votingSessionService;
  private final GameRepository gameRepository;

  @GetMapping("/current")
  @Operation(
      summary = "Get current active voting session",
      description = "Returns the currently active voting session for the game, if any")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Active voting session found",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = VotingSessionDto.class))),
        @ApiResponse(responseCode = "204", description = "No active voting session"),
        @ApiResponse(responseCode = "404", description = "Game not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
      })
  public ResponseEntity<VotingSessionDto> getCurrentSession(
      @Parameter(description = "Game ID", required = true) @PathVariable UUID gameId) {

    log.info("Getting current voting session for game {}", gameId);

    Game game =
        gameRepository
            .findById(gameId)
            .orElseThrow(() -> new GameNotFoundException("Game not found"));

    Optional<VotingSession> sessionOpt = votingSessionService.getCurrentActiveSession(game);

    if (sessionOpt.isEmpty()) {
      log.debug("No active voting session for game {}", gameId);
      return ResponseEntity.noContent().build();
    }

    VotingSession session = sessionOpt.get();
    VotingSessionDto dto = votingSessionService.toDto(session);

    log.info("Returning active voting session {} for game {}", session.getId(), gameId);

    return ResponseEntity.ok(dto);
  }

  @PostMapping("/vote")
  @Operation(
      summary = "Cast a vote",
      description = "Cast a vote for a target player in the current voting session")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Vote cast successfully",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = CastVoteResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request or voting not allowed"),
        @ApiResponse(responseCode = "404", description = "Game or session not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
      })
  public ResponseEntity<CastVoteResponse> castVote(
      @Parameter(description = "Game ID", required = true) @PathVariable UUID gameId,
      @Parameter(description = "Vote request", required = true) @Valid @RequestBody
          CastVoteRequest request) {

    User currentUser = getCurrentUser();
    log.info(
        "User {} casting vote in game {} for session {}",
        currentUser.getUsername(),
        gameId,
        request.getVotingSessionId());

    Game game =
        gameRepository
            .findById(gameId)
            .orElseThrow(() -> new GameNotFoundException("Game not found"));

    CastVoteResponse response =
        votingSessionService.castVote(
            request.getVotingSessionId(), currentUser.getId(), request.getTargetUserId());

    if (response.isSuccess()) {
      log.info(
          "Vote cast successfully by user {} in session {}",
          currentUser.getUsername(),
          request.getVotingSessionId());
    } else {
      log.warn(
          "Vote failed for user {} in session {}: {}",
          currentUser.getUsername(),
          request.getVotingSessionId(),
          response.getMessage());
    }

    return ResponseEntity.ok(response);
  }

  @GetMapping("/results")
  @Operation(
      summary = "Get voting results",
      description = "Get the aggregated voting results for a specific voting session")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Results retrieved successfully",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = VoteResultDto.class))),
        @ApiResponse(responseCode = "404", description = "Session not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
      })
  public ResponseEntity<List<VoteResultDto>> getResults(
      @Parameter(description = "Game ID", required = true) @PathVariable UUID gameId,
      @Parameter(description = "Voting session ID", required = true) @RequestParam UUID sessionId) {

    log.info("Getting results for voting session {}", sessionId);

    List<VoteResultDto> results = votingSessionService.getResults(sessionId);

    log.info("Returning {} vote results for session {}", results.size(), sessionId);

    return ResponseEntity.ok(results);
  }

  private User getCurrentUser() {
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
}
