package com.mafia.gameservice.services;

import com.mafia.gameservice.dto.voting.CastVoteResponse;
import com.mafia.gameservice.dto.voting.VoteResultDto;
import com.mafia.gameservice.dto.voting.VotingSessionDto;
import com.mafia.gameservice.enums.GamePhase;
import com.mafia.gameservice.enums.GameRole;
import com.mafia.gameservice.enums.GameStatus;
import com.mafia.gameservice.enums.VotingStatus;
import com.mafia.gameservice.models.*;
import com.mafia.gameservice.orchestration.GameOrchestrator;
import com.mafia.gameservice.repositories.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class VotingSessionService {

    private final VotingSessionRepository votingSessionRepository;
    private final GameVoteRepository gameVoteRepository;
    private final GamePlayerRepository gamePlayerRepository;
    private final UserRepository userRepository;
    private final GameRepository gameRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final GameOrchestrator gameOrchestrator;

    public VotingSessionService(
            VotingSessionRepository votingSessionRepository,
            GameVoteRepository gameVoteRepository,
            GamePlayerRepository gamePlayerRepository,
            UserRepository userRepository,
            GameRepository gameRepository,
            SimpMessagingTemplate messagingTemplate,
            @Lazy GameOrchestrator gameOrchestrator) {
        this.votingSessionRepository = votingSessionRepository;
        this.gameVoteRepository = gameVoteRepository;
        this.gamePlayerRepository = gamePlayerRepository;
        this.userRepository = userRepository;
        this.gameRepository = gameRepository;
        this.messagingTemplate = messagingTemplate;
        this.gameOrchestrator = gameOrchestrator;
    }

    /**
     * Starts a new voting session for the given game and phase.
     */
    @Transactional
    public VotingSession startVotingSession(Game game, GamePhase phase) {
        log.info("Starting voting session for game {} phase {} day {}",
                game.getId(), phase, game.getCurrentDayNumber());

        // Check no active session already exists for this phase/day
        Optional<VotingSession> existing = votingSessionRepository
                .findByGameAndPhaseAndDayNumberAndStatus(game, phase, game.getCurrentDayNumber(), VotingStatus.ACTIVE);
        if (existing.isPresent()) {
            log.warn("Active voting session already exists for game {}", game.getId());
            throw new IllegalStateException("Active voting session already exists");
        }

        // All alive players submit a vote (at night, citizen votes are decoys — only mafia votes count).
        int eligibleVoters = gamePlayerRepository.countByGameAndIsAlive(game, true);

        if (eligibleVoters == 0) {
            throw new IllegalStateException("No eligible voters for phase " + phase);
        }

        int discussionTime = game.getRoom().getDiscussionTimeSeconds();

        VotingSession session = new VotingSession();
        session.setGame(game);
        session.setPhase(phase);
        session.setDayNumber(game.getCurrentDayNumber());
        session.setStartedAt(LocalDateTime.now());
        session.setEndsAt(LocalDateTime.now().plusSeconds(discussionTime));
        session.setStatus(VotingStatus.ACTIVE);
        session.setTotalEligibleVoters(eligibleVoters);
        session.setVotesReceived(0);

        VotingSession saved = votingSessionRepository.save(session);
        log.info("Voting session created: {}", saved.getId());

        broadcastVotingUpdate(saved);
        return saved;
    }

    /**
     * Returns the currently active session for the given game.
     */
    @Transactional(readOnly = true)
    public Optional<VotingSession> getCurrentActiveSession(Game game) {
        return votingSessionRepository.findActiveSessionByGame(game);
    }

    /**
     * Casts a vote. Validates eligibility, saves the vote, and auto-completes
     * the session if all eligible voters have voted.
     */
    @Transactional
    public CastVoteResponse castVote(UUID sessionId, UUID voterUserId, UUID targetUserId) {
        log.info("Casting vote: session={}, voter={}, target={}", sessionId, voterUserId, targetUserId);

        VotingSession session = votingSessionRepository.findById(sessionId).orElse(null);
        if (session == null) {
            return CastVoteResponse.error("Voting session not found");
        }

        if (session.getStatus() != VotingStatus.ACTIVE) {
            return CastVoteResponse.error("Voting session is not active");
        }

        if (LocalDateTime.now().isAfter(session.getEndsAt())) {
            expireSession(session);
            return CastVoteResponse.error("Voting time has expired");
        }

        // Validate voter is in this game and alive
        GamePlayer voter = gamePlayerRepository.findByGameAndUser_Id(session.getGame(), voterUserId)
                .orElse(null);
        if (voter == null) {
            return CastVoteResponse.error("Voter not found in game");
        }
        if (!voter.isAlive()) {
            return CastVoteResponse.error("Dead players cannot vote");
        }

        // Check target is alive
        GamePlayer target = gamePlayerRepository.findByGameAndUser_Id(session.getGame(), targetUserId)
                .orElse(null);
        if (target == null || !target.isAlive()) {
            return CastVoteResponse.error("Target player not found or is already dead");
        }

        // Check not already voted
        Optional<GameVote> existingVote = gameVoteRepository.findByVotingSessionAndVoterId(session, voterUserId);
        if (existingVote.isPresent()) {
            return CastVoteResponse.error("You have already voted");
        }

        // Save vote
        GameVote vote = new GameVote();
        vote.setGame(session.getGame());
        vote.setVotingSession(session);
        vote.setPhase(session.getPhase());
        vote.setDayNumber(session.getDayNumber());
        vote.setVoterId(voterUserId);
        vote.setTargetUserId(targetUserId);
        // Night: everyone clicks, but only mafia votes count toward elimination.
        boolean countsForResult = session.getPhase() != GamePhase.NIGHT_VOTE
                || voter.getAssignedRole() == GameRole.MAFIA;
        vote.setValid(countsForResult);
        gameVoteRepository.save(vote);

        session.setVotesReceived(session.getVotesReceived() + 1);
        votingSessionRepository.save(session);

        broadcastVotingUpdate(session);

        // Auto-complete when all eligible voters have voted
        if (session.getVotesReceived() >= session.getTotalEligibleVoters()) {
            log.info("All players voted - completing session {}", sessionId);
            completeVoting(session);
        }

        return CastVoteResponse.success(targetUserId);
    }

    /**
     * Returns aggregated vote counts for the given session.
     */
    @Transactional(readOnly = true)
    public List<VoteResultDto> getResults(UUID sessionId) {
        VotingSession session = votingSessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Voting session not found"));

        List<Object[]> raw = gameVoteRepository.countVotesByTarget(session);
        return raw.stream().map(row -> {
            UUID targetId = (UUID) row[0];
            int count = ((Number) row[1]).intValue();
            String username = userRepository.findById(targetId)
                    .map(User::getUsername)
                    .orElse("unknown");
            return new VoteResultDto(targetId, username, count);
        }).collect(Collectors.toList());
    }

    /**
     * Builds a VotingSessionDto from a VotingSession entity.
     */
    public VotingSessionDto toDto(VotingSession session) {
        long remaining = ChronoUnit.SECONDS.between(LocalDateTime.now(), session.getEndsAt());
        remaining = Math.max(0, remaining);

        List<Object[]> raw = session.getPhase() == GamePhase.NIGHT_VOTE
                ? gameVoteRepository.countAllVotesByTarget(session)
                : gameVoteRepository.countVotesByTarget(session);
        Map<UUID, Integer> voteMap = new LinkedHashMap<>();
        for (Object[] row : raw) {
            voteMap.put((UUID) row[0], ((Number) row[1]).intValue());
        }

        return new VotingSessionDto(
                session.getId(),
                session.getPhase().name(),
                session.getDayNumber(),
                remaining,
                session.getTotalEligibleVoters(),
                session.getVotesReceived(),
                voteMap
        );
    }

    /**
     * Expires an active session (called by the scheduler on timeout).
     */
    @Transactional
    public void expireSession(VotingSession session) {
        if (session.getStatus() != VotingStatus.ACTIVE) {
            log.warn("Session {} already processed with status {}", session.getId(), session.getStatus());
            return;
        }

        log.info("Expiring voting session {}", session.getId());
        session.setStatus(VotingStatus.EXPIRED);
        votingSessionRepository.save(session);

        broadcastVotingExpired(session);
        long voteCount = gameVoteRepository.countByVotingSession(session);
        if (voteCount > 0) {
            // Resolve using valid (mafia) votes only; citizen decoys are ignored for elimination
            completeVoting(session);
        } else {
            buildNoEliminationResult(session);
        }
    }

    // ==================== PRIVATE HELPERS ====================

    @Transactional
    private void completeVoting(VotingSession session) {
        log.info("Completing voting session {}", session.getId());

        // Aggregate votes
        List<Object[]> voteCounts = gameVoteRepository.countVotesByTarget(session);

        // Find max votes
        int maxVotes = voteCounts.stream()
                .mapToInt(row -> ((Number) row[1]).intValue())
                .max().orElse(0);

        List<UUID> topTargets = voteCounts.stream()
                .filter(row -> ((Number) row[1]).intValue() == maxVotes)
                .map(row -> (UUID) row[0])
                .collect(Collectors.toList());

        boolean isTie = topTargets.size() > 1;
        User eliminatedUser = null;

        if (maxVotes == 0) {
            // No votes cast
            session.setStatus(VotingStatus.COMPLETED);
            session.setTie(false);
            votingSessionRepository.save(session);
            buildNoEliminationResult(session);
            return;
        }

        UUID eliminatedId;
        if (isTie) {
            if (session.getPhase() == GamePhase.DAY_VOTE) {
                // Tie in day vote → no elimination
                session.setStatus(VotingStatus.COMPLETED);
                session.setTie(true);
                votingSessionRepository.save(session);
                buildNoEliminationResult(session);
                return;
            } else {
                // Tie in night vote → random pick
                eliminatedId = topTargets.get(new Random().nextInt(topTargets.size()));
            }
        } else {
            eliminatedId = topTargets.get(0);
        }

        eliminatedUser = userRepository.findById(eliminatedId).orElse(null);

        // Mark player as dead
        if (eliminatedUser != null) {
            gamePlayerRepository.findByGameAndUser_Id(session.getGame(), eliminatedId)
                    .ifPresent(gp -> {
                        gp.setAlive(false);
                        gamePlayerRepository.save(gp);
                        log.info("Player {} eliminated", gp.getGameNick());
                    });
        }

        session.setStatus(VotingStatus.COMPLETED);
        session.setResultUser(eliminatedUser);
        session.setTie(isTie);
        votingSessionRepository.save(session);

        // Build VotingResult and delegate to orchestrator
        List<VoteResult> voteResultModels = buildVoteResultList(session, voteCounts);
        VotingResult result = buildVotingResult(eliminatedUser, isTie, voteResultModels);

        broadcastVotingComplete(session, result);
        gameOrchestrator.handleVotingCompleted(session, result);
    }

    private void buildNoEliminationResult(VotingSession session) {
        VotingResult result = VotingResult.noElimination();
        broadcastVotingComplete(session, result);
        gameOrchestrator.handleVotingCompleted(session, result);
    }

    private VotingResult buildVotingResult(User eliminated, boolean isTie, List<VoteResult> results) {
        if (eliminated == null && !isTie) {
            return VotingResult.noElimination();
        }
        if (isTie && eliminated == null) {
            return VotingResult.tie(results);
        }
        if (isTie) {
            return VotingResult.tieRandomElimination(eliminated, results);
        }
        return VotingResult.elimination(eliminated, results);
    }

    private List<VoteResult> buildVoteResultList(VotingSession session, List<Object[]> voteCounts) {
        return voteCounts.stream().map(row -> {
            UUID targetId = (UUID) row[0];
            int count = ((Number) row[1]).intValue();
            User target = userRepository.findById(targetId).orElse(null);
            VoteResult vr = new VoteResult();
            vr.setVotingSession(session);
            vr.setTargetUser(target);
            vr.setVoteCount(count);
            return vr;
        }).collect(Collectors.toList());
    }

    private void broadcastVotingUpdate(VotingSession session) {
        try {
            String roomCode = session.getGame().getRoom().getRoomCode();
            VotingSessionDto dto = toDto(session);
            String topic = "/topic/game/" + roomCode + "/voting";
            messagingTemplate.convertAndSend(topic, (Object) dto);
        } catch (Exception e) {
            log.error("Error broadcasting voting update", e);
        }
    }

    private void broadcastVotingComplete(VotingSession session, VotingResult result) {
        try {
            String roomCode = session.getGame().getRoom().getRoomCode();
            Map<String, Object> payload = new HashMap<>();
            payload.put("type", "voting_complete");
            payload.put("sessionId", session.getId());
            payload.put("phase", session.getPhase().name());
            payload.put("dayNumber", session.getDayNumber());
            payload.put("eliminated", result.getEliminatedUser() != null
                    ? result.getEliminatedUser().getUsername() : null);
            payload.put("eliminatedUserId", result.getEliminatedUser() != null
                    ? result.getEliminatedUser().getId() : null);
            payload.put("isTie", result.isTie());
            payload.put("resultType", result.getResultType().name());
            String topic = "/topic/game/" + roomCode + "/voting/result";
            messagingTemplate.convertAndSend(topic, (Object) payload);
        } catch (Exception e) {
            log.error("Error broadcasting voting complete", e);
        }
    }

    private void broadcastVotingExpired(VotingSession session) {
        try {
            String roomCode = session.getGame().getRoom().getRoomCode();
            Map<String, Object> payload = new HashMap<>();
            payload.put("type", "voting_expired");
            payload.put("sessionId", session.getId());
            payload.put("phase", session.getPhase().name());
            String topic = "/topic/game/" + roomCode + "/voting/expired";
            messagingTemplate.convertAndSend(topic, (Object) payload);
        } catch (Exception e) {
            log.error("Error broadcasting voting expired", e);
        }
    }
}
