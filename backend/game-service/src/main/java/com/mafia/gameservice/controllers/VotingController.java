package com.mafia.gameservice.controllers;

import com.mafia.gameservice.dto.voting.CastVoteRequest;
import com.mafia.gameservice.dto.voting.CastVoteResponse;
import com.mafia.gameservice.dto.voting.VoteResultDto;
import com.mafia.gameservice.dto.voting.VotingSessionDto;
import com.mafia.gameservice.models.Game;
import com.mafia.gameservice.models.User;
import com.mafia.gameservice.models.VotingSession;
import com.mafia.gameservice.repositories.GameRepository;
import com.mafia.gameservice.services.VotingSessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/games/{gameId}/voting")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
@Slf4j
public class VotingController {

    private final VotingSessionService votingSessionService;
    private final GameRepository gameRepository;

    @GetMapping("/current")
    public ResponseEntity<VotingSessionDto> getCurrentSession(@PathVariable UUID gameId) {
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new IllegalArgumentException("Game not found: " + gameId));

        Optional<VotingSession> sessionOpt = votingSessionService.getCurrentActiveSession(game);
        if (sessionOpt.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(votingSessionService.toDto(sessionOpt.get()));
    }

    @PostMapping("/vote")
    public ResponseEntity<CastVoteResponse> castVote(
            @PathVariable UUID gameId,
            @Valid @RequestBody CastVoteRequest request) {

        User user = getCurrentUser();
        log.info("User {} casting vote in game {}", user.getUsername(), gameId);

        // Validate game exists
        gameRepository.findById(gameId)
                .orElseThrow(() -> new IllegalArgumentException("Game not found: " + gameId));

        CastVoteResponse response = votingSessionService.castVote(
                request.getVotingSessionId(), user.getId(), request.getTargetUserId());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/results")
    public ResponseEntity<List<VoteResultDto>> getResults(
            @PathVariable UUID gameId,
            @RequestParam UUID sessionId) {

        return ResponseEntity.ok(votingSessionService.getResults(sessionId));
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (User) auth.getPrincipal();
    }
}
