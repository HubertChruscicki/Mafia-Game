package com.mafia.gameservice.services;

import com.mafia.gameservice.dto.VoteRequest;
import com.mafia.gameservice.dto.VoteResponse;
import com.mafia.gameservice.models.*;
import com.mafia.gameservice.enums.GamePhase;
import com.mafia.gameservice.enums.GameRole;
import com.mafia.gameservice.repositories.GameVoteRepository;
import com.mafia.gameservice.repositories.PlayerGameStateRepository;
import com.mafia.gameservice.repositories.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class GameEngineService {

    private final PlayerGameStateRepository players;
    private final GameVoteRepository votes;
    private final UserRepository users;
    private final SimpMessagingTemplate messaging;
    private final GameRoomService gameRoomService;
    private final Random random = new Random();

    public GameEngineService(PlayerGameStateRepository players,
                             GameVoteRepository votes,
                             UserRepository users,
                             SimpMessagingTemplate messaging,
                             GameRoomService gameRoomService) {
        this.players = players;
        this.votes = votes;
        this.users = users;
        this.messaging = messaging;
        this.gameRoomService = gameRoomService;
    }

    @Transactional
    public void startGame(String roomCode, List<UUID> userIds) {
        int mafiaCount = Math.max(1, Math.round(userIds.size() * 0.25f));
        var shuffled = new ArrayList<>(userIds);
        Collections.shuffle(shuffled, random);
        var mafiaSet = new HashSet<>(shuffled.subList(0, mafiaCount));

        votes.deleteByRoomCodeAndPhase(roomCode, GamePhase.DAY_VOTE);
        votes.deleteByRoomCodeAndPhase(roomCode, GamePhase.NIGHT_VOTE);

        for (UUID uid : userIds) {
            var existing = players.findByRoomCodeAndUserId(roomCode, uid);
            if (existing == null)
                existing = new GamePlayer();
            existing.setAssignedRole(mafiaSet.contains(uid) ? GameRole.MAFIA : GameRole.CITIZEN);
            existing.setAlive(true);
            players.save(existing);
        }
        
        // Broadcast game start to all players in the room
        broadcastGameStart(roomCode, userIds.size(), mafiaCount);
    }

    /**
     * Broadcast game start event to all players in room
     */
    private void broadcastGameStart(String roomCode, int totalPlayers, int mafiaCount) {
        try {
            messaging.convertAndSend("/topic/game/" + roomCode + "/updated", (Object) Map.of(
                    "type", "game_started",
                    "roomCode", roomCode,
                    "totalPlayers", totalPlayers,
                    "mafiaCount", mafiaCount,
                    "message", "Game started! %d players, %d mafia".formatted(totalPlayers, mafiaCount)
            ));
        } catch (Exception e) {
            // Log but don't fail if broadcast fails
        }
    }

    /**
     * Process a vote from a player during day or night phase.
     * Returns vote status and checks if voting is complete.
     */
    @Transactional
    public VoteResponse doVote(String roomCode, VoteRequest request) {
        // Validate inputs
        validateVoteRequest(request, roomCode);

        // Resolve users
        var voterUser = users.findByUsername(request.getVoterUsername())
                .orElseThrow(() -> new IllegalStateException("Voter user not found: " + request.getVoterUsername()));
        var targetUser = users.findByUsername(request.getTargetUsername())
                .orElseThrow(() -> new IllegalStateException("Target user not found: " + request.getTargetUsername()));

        // Validate voter state
        var voter = players.findByRoomCodeAndUserId(roomCode, voterUser.getId());
        if (voter == null) {
            throw new IllegalStateException("Voter is not in this room");
        }
        if (!voter.isAlive()) {
            throw new IllegalStateException("Voter is not alive");
        }

        // Validate target state
        var target = players.findByRoomCodeAndUserId(roomCode, targetUser.getId());
        if (target == null) {
            throw new IllegalStateException("Target is not in this room");
        }
        if (!target.isAlive()) {
            throw new IllegalStateException("Target is not alive");
        }

        // Validate phase-specific permissions
        validatePhasePermissions(request.getGamePhase(), voter.getAssignedRole());

        // Upsert vote
        var existingVote = votes.findByRoomCodeAndPhaseAndVoterId(
                roomCode, request.getGamePhase(), voterUser.getId());
        
        GameVote vote = existingVote.orElseGet(() -> {
            GameVote newVote = new GameVote();
            newVote.setVoterId(voterUser.getId());
            newVote.setPhase(request.getGamePhase());
            newVote.setDayNumber(request.getDayNumber());
            return newVote;
        });
        
        vote.setTargetUserId(target.getUser().getId());
        vote.setVotedAt(LocalDateTime.now());
        votes.save(vote);

        // Get voting statistics
        long votesReceived = votes.countByRoomCodeAndPhaseAndDayNumber(
                roomCode, request.getGamePhase(), request.getDayNumber());
        long totalAlive = players.countByRoomCodeAndAliveTrue(roomCode);

        // Broadcast vote update
        broadcastVoteUpdate(roomCode, request.getGamePhase(), request.getDayNumber(), votesReceived, totalAlive);

        // Check if voting is complete
        boolean votingComplete = votesReceived >= totalAlive;
        VoteResponse response = new VoteResponse();
        response.setStatus("VOTE_RECEIVED");
        response.setCurrentPhase(request.getGamePhase());
        response.setDayNumber(request.getDayNumber());
        response.setVotesReceived(votesReceived);
        response.setTotalPlayersAlive(totalAlive);
        response.setVotingComplete(votingComplete);
        response.setMessage("Vote received. %d/%d players have voted.".formatted(votesReceived, totalAlive));

        // If voting is complete, resolve and transition
        if (votingComplete) {
            var eliminationResult = resolveVoting(roomCode, request.getGamePhase(), request.getDayNumber());
            if (eliminationResult != null) {
                response.setStatus("VOTING_COMPLETE");
                response.setEliminatedPlayerId(eliminationResult.getKey());
                response.setEliminatedPlayerUsername(eliminationResult.getValue());
                response.setMessage(String.format("Voting complete. %s has been eliminated.", eliminationResult.getValue()));
                
                // Transition to next phase
                transitionPhase(roomCode, request.getGamePhase(), request.getDayNumber());
            }
        }

        return response;
    }

    /**
     * Validate vote request fields
     */
    private void validateVoteRequest(VoteRequest request, String roomCode) {
        if (roomCode == null || roomCode.isBlank()) {
            throw new IllegalStateException("Room code is required");
        }
        if (request.getVoterUsername() == null || request.getVoterUsername().isBlank()) {
            throw new IllegalStateException("Voter username is required");
        }
        if (request.getTargetUsername() == null || request.getTargetUsername().isBlank()) {
            throw new IllegalStateException("Target username is required");
        }
        if (request.getGamePhase() == null) {
            throw new IllegalStateException("Game phase is required");
        }
        if (request.getVoterRole() == null) {
            throw new IllegalStateException("Voter role is required");
        }
        if (request.getDayNumber() == null || request.getDayNumber() < 1) {
            throw new IllegalStateException("Valid day number is required");
        }
    }

    /**
     * Validate that voter has permission to vote in this phase
     */
    private void validatePhasePermissions(GamePhase phase, GameRole voterRole) {
        if (phase == GamePhase.NIGHT_VOTE && voterRole != GameRole.MAFIA) {
            throw new IllegalStateException("Only Mafia can vote during night phase");
        }
        if (phase == GamePhase.DAY_VOTE && voterRole == null) {
            throw new IllegalStateException("Invalid voter role for day phase");
        }
    }

    /**
     * Resolve voting and determine elimination
     */
    @Transactional
    private Map.Entry<UUID, String> resolveVoting(String roomCode, GamePhase phase, Integer dayNumber) {
        var voteList = votes.findByRoomCodeAndPhaseAndDayNumber(roomCode, phase, dayNumber);
        if (voteList.isEmpty()) {
            return null;
        }

        // Count votes by target
        Map<UUID, Long> voteCounts = voteList.stream()
                .collect(Collectors.groupingBy(GameVote::getTargetUserId, Collectors.counting()));

        long maxVotes = voteCounts.values().stream()
                .mapToLong(Long::longValue)
                .max()
                .orElse(0);

        // Find all targets with max votes
        var topTargets = voteCounts.entrySet().stream()
                .filter(e -> e.getValue() == maxVotes)
                .map(Map.Entry::getKey)
                .toList();

        UUID eliminatedId = null;

        if (topTargets.size() == 1) {
            // Clear winner
            eliminatedId = topTargets.getFirst();
        } else if (phase == GamePhase.NIGHT_VOTE) {
            // Night tie: random elimination from tied targets
            eliminatedId = topTargets.get(random.nextInt(topTargets.size()));
        } else if (phase == GamePhase.DAY_VOTE) {
            // Day tie: no elimination
            return null;
        }

        if (eliminatedId != null) {
            eliminate(roomCode, eliminatedId);
            var eliminatedUser = users.findById(eliminatedId).orElse(null);
            String eliminatedUsername = eliminatedUser != null ? eliminatedUser.getUsername() : "Unknown";
            return new AbstractMap.SimpleEntry<>(eliminatedId, eliminatedUsername);
        }

        return null;
    }

    /**
     * Transition to next phase
     */
    @Transactional
    private void transitionPhase(String roomCode, GamePhase currentPhase, Integer dayNumber) {
        if (currentPhase == GamePhase.DAY_VOTE) {
            // Day -> Night
            broadcastPhaseTransition(roomCode, GamePhase.NIGHT_VOTE, dayNumber);
        } else if (currentPhase == GamePhase.NIGHT_VOTE) {
            // Night -> Next Day
            broadcastPhaseTransition(roomCode, GamePhase.DAY_VOTE, dayNumber + 1);
        }
    }

    /**
     * Broadcast vote update to all players in room
     */
    private void broadcastVoteUpdate(String roomCode, GamePhase phase, Integer dayNumber, long votesReceived, long totalAlive) {
        try {
            messaging.convertAndSend("/topic/rooms/" + roomCode, (Object) Map.of(
                    "type", "vote_update",
                    "phase", phase.name(),
                    "dayNumber", dayNumber,
                    "votesReceived", votesReceived,
                    "totalAlive", totalAlive,
                    "progress", "%d/%d".formatted(votesReceived, totalAlive)
            ));
        } catch (Exception ignored) {}
    }

    /**
     * Broadcast phase transition
     */
    private void broadcastPhaseTransition(String roomCode, GamePhase newPhase, Integer dayNumber) {
        try {
            messaging.convertAndSend("/topic/rooms/" + roomCode, (Object) Map.of(
                    "type", "phase_transition",
                    "newPhase", newPhase.name(),
                    "dayNumber", dayNumber,
                    "message", "Transitioning to %s (Day %d)".formatted(newPhase.name(), dayNumber)
            ));
        } catch (Exception ignored) {}
    }

    /**
     * Mark player as eliminated
     */
    @Transactional
    protected void eliminate(String roomCode, UUID userId) {
        var player = players.findByRoomCodeAndUserId(roomCode, userId);
        if (player != null && player.isAlive()) {
            player.setAlive(false);
            players.save(player);
        }
    }

    /**
     * Legacy method for backward compatibility
     */
    @Transactional
    public Optional<UUID> closeVote(String roomCode) {
        var dayResult = resolveDayElimination(roomCode);
        if (dayResult.isPresent()) {
            votes.deleteByRoomCodeAndPhase(roomCode, GamePhase.DAY_VOTE);
            try {
                messaging.convertAndSend("/topic/rooms/" + roomCode, (Object) Map.of(
                        "type", "vote_closed",
                        "phase", GamePhase.DAY_VOTE.name(),
                        "victim", dayResult.get().toString()
                ));
            } catch (Exception ignored) {}
            return dayResult;
        }
        var nightResult = resolveNightKill(roomCode);
        nightResult.ifPresent(v -> {
            votes.deleteByRoomCodeAndPhase(roomCode, GamePhase.NIGHT_VOTE);
            try {
                messaging.convertAndSend("/topic/rooms/" + roomCode, (Object) Map.of(
                        "type", "vote_closed",
                        "phase", GamePhase.NIGHT_VOTE.name(),
                        "victim", v.toString()
                ));
            } catch (Exception ignored) {}
        });
        return nightResult;
    }

    @Transactional
    public Optional<UUID> resolveDayElimination(String roomCode) {
        var list = votes.findByRoomCodeAndPhase(roomCode, GamePhase.DAY_VOTE);
        if (list.isEmpty())
            return Optional.empty();

        Map<UUID, Long> counts = list.stream()
                .collect(Collectors.groupingBy(GameVote::getTargetUserId, Collectors.counting()));

        long max = counts.values().stream().mapToLong(Long::longValue).max().orElse(0);
        var top = counts.entrySet().stream()
                .filter(e -> e.getValue() == max)
                .map(Map.Entry::getKey)
                .toList();

        if (top.size() == 1) {
            eliminate(roomCode, top.getFirst());
            return Optional.of(top.getFirst());
        }
        return Optional.empty();
    }

    @Transactional
    public Optional<UUID> resolveNightKill(String roomCode) {
        var list = votes.findByRoomCodeAndPhase(roomCode, GamePhase.NIGHT_VOTE);
        if (list.isEmpty())
            return Optional.empty();

        var mafiaIds = players.findByRoomCodeAndAliveTrueAndRole(roomCode, GameRole.MAFIA)
                .stream().map(gp -> gp.getUser().getId()).collect(Collectors.toSet());

        Map<UUID, Long> counts = list.stream()
                .filter(v -> mafiaIds.contains(v.getVoterId()))
                .collect(Collectors.groupingBy(GameVote::getTargetUserId, Collectors.counting()));

        if (counts.isEmpty())
            return Optional.empty();

        long max = counts.values().stream().mapToLong(Long::longValue).max().orElse(0);
        var top = counts.entrySet().stream()
                .filter(e -> e.getValue() == max)
                .map(Map.Entry::getKey)
                .toList();

        UUID victim = top.size() == 1 ? top.getFirst() : top.get(random.nextInt(top.size()));
        eliminate(roomCode, victim);
        return Optional.of(victim);
    }

    @Transactional
    public void submitVoteRequireAll(String roomCode, UUID voterId, GamePhase phase, UUID targetUserId) {
        var voter = players.findByRoomCodeAndUserId(roomCode, voterId);
        if (voter == null || !voter.isAlive())
            throw new IllegalStateException("Voter not alive/in room");

        var target = players.findByRoomCodeAndUserId(roomCode, targetUserId);
        if (target == null || !target.isAlive())
            throw new IllegalStateException("Target not alive/in room");

        var existing = votes.findByRoomCodeAndPhaseAndVoterId(roomCode, phase, voterId);
        GameVote v = existing.orElseGet(() -> {
            GameVote newVote = new GameVote();
            newVote.setVoterId(voterId);
            newVote.setPhase(phase);
            newVote.setDayNumber(1);
            return newVote;
        });
        v.setTargetUserId(targetUserId);
        votes.save(v);

        long totalAlive = players.countByRoomCodeAndAliveTrue(roomCode);
        long cast = votes.countByRoomCodeAndPhase(roomCode, phase);
        if (cast >= totalAlive) {
            if (phase == GamePhase.DAY_VOTE) {
                resolveDayElimination(roomCode);
            } else if (phase == GamePhase.NIGHT_VOTE) {
                resolveNightKill(roomCode);
            }
        }
    }
}
