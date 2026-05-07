package com.mafia.gameservice.orchestration;
import com.mafia.gameservice.enums.GamePhase;
import com.mafia.gameservice.enums.GameRole;
import com.mafia.gameservice.enums.GameRoomStatus;
import com.mafia.gameservice.enums.GameStatus;
import com.mafia.gameservice.models.Game;
import com.mafia.gameservice.models.GamePlayer;
import com.mafia.gameservice.models.GameRoom;
import com.mafia.gameservice.models.VotingSession;
import com.mafia.gameservice.repositories.GamePlayerRepository;
import com.mafia.gameservice.repositories.GameRepository;
import com.mafia.gameservice.repositories.GameRoomRepository;
import com.mafia.gameservice.services.GameEventPublisher;
import com.mafia.gameservice.services.VotingResult;
import com.mafia.gameservice.statemachine.game.GamePhaseEvent;
import com.mafia.gameservice.statemachine.game.GamePhaseState;
import com.mafia.gameservice.statemachine.game.GamePhaseStateMachine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Centralny orchestrator gry.
 *
 * Odpowiedzialny za:
 * - Koordynację przejść między fazami gry
 * - Sprawdzanie warunków wygranej
 * - Rozpoczynanie nowych sesji głosowania
 * - Broadcast zmian stanu przez WebSocket
 *
 * Jest to główny punkt kontroli przepływu gry.
 */
@Service
@Slf4j
public class GameOrchestrator {

    private final GamePhaseStateMachine phaseStateMachine;
    private final GameRepository gameRepository;
    private final GameRoomRepository gameRoomRepository;
    private final GamePlayerRepository gamePlayerRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final GameEventPublisher eventPublisher;

    // Używamy @Lazy aby uniknąć circular dependency
    public GameOrchestrator(
            GamePhaseStateMachine phaseStateMachine,
            GameRepository gameRepository,
            GameRoomRepository gameRoomRepository,
            GamePlayerRepository gamePlayerRepository,
            SimpMessagingTemplate messagingTemplate,
            GameEventPublisher eventPublisher) {
        this.phaseStateMachine = phaseStateMachine;
        this.gameRepository = gameRepository;
        this.gameRoomRepository = gameRoomRepository;
        this.gamePlayerRepository = gamePlayerRepository;
        this.messagingTemplate = messagingTemplate;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Obsługuje zakończenie głosowania.
     * Wywoływane przez VotingSessionService po zakończeniu sesji głosowania.
     */
    @Transactional
    public void handleVotingCompleted(VotingSession session, VotingResult result) {
        Game game = session.getGame();
        String roomCode = game.getRoom().getRoomCode();

        log.info("=== VOTING COMPLETED === Game: {}, Phase: {}, Result: {}",
                game.getId(), session.getPhase(), result.getDescription());

        // 1. Przejdź do fazy wyników
        GamePhaseState currentState = phaseStateMachine.getCurrentState(game);
        phaseStateMachine.transition(game, GamePhaseEvent.VOTING_COMPLETED);
        gameRepository.save(game);

        // 2. Broadcast wynik głosowania
        broadcastVotingResult(game, result);

        // 3. Sprawdź warunki wygranej
        WinConditionResult winCheck = checkWinConditions(game);

        if (winCheck.hasWinner()) {
            log.info("=== WIN CONDITION MET === Winner: {}", winCheck.getWinner());
            handleGameEnd(game, winCheck);
        } else {
            // 4. Zaplanuj przejście do następnej fazy po krótkim opóźnieniu
            // (daje czas na wyświetlenie wyniku)
            scheduleNextPhase(game, result);
        }
    }

    /**
     * Obsługuje wygaśnięcie głosowania (timeout).
     */
    @Transactional
    public void handleVotingExpired(VotingSession session) {
        log.info("=== VOTING EXPIRED === Session: {}", session.getId());
        //TODO WYGASIC SESJE (SERIWS DO GLOSOWANIA)
    }

    /**
     * Rozpoczyna następną fazę gry.
     * Wywoływane po przetworzeniu wyniku głosowania.
     */
    @Transactional
    public void startNextPhase(Game game) {
        log.info("=== STARTING NEXT PHASE === Game: {}", game.getId());

        // Przejdź z fazy wyników do fazy głosowania
        phaseStateMachine.transition(game, GamePhaseEvent.RESULT_PROCESSED);

        GamePhaseState newState = phaseStateMachine.getCurrentState(game);
        log.info("New phase state: {}", newState);

        // Zwiększ numer dnia jeśli przechodzimy do nocy
        if (newState == GamePhaseState.NIGHT_VOTING) {
            // Nowa noc = nowy dzień (dzień kończy się po głosowaniu dziennym)
            // Ale pierwszy dzień zaczyna się od nocy, więc nie zwiększamy przy pierwszej nocy
            if (game.getCurrentDayNumber() > 0) {
                // Nie zwiększamy - dzień zwiększa się przy przejściu do DAY_VOTING
            }
        } else if (newState == GamePhaseState.DAY_VOTING) {
            // Zwiększ numer dnia przy przejściu do głosowania dziennego
            game.setCurrentDayNumber(game.getCurrentDayNumber() + 1);
        }

        gameRepository.save(game);

        // Rozpocznij nową sesję głosowania
        startVotingSession(game, newState);

        // Broadcast zmianę fazy
        broadcastPhaseChange(game, newState);
    }

    /**
     * Rozpoczyna sesję głosowania dla danej fazy.
     */
    private void startVotingSession(Game game, GamePhaseState phaseState) {
        GamePhase gamePhase = phaseStateMachine.mapToGamePhase(phaseState);
        int discussionTime = game.getRoom().getDiscussionTimeSeconds();

        log.info("Starting voting session: phase={}, discussionTime={}s", gamePhase, discussionTime);

        //TODO START SESJI
    }

    /**
     * Sprawdza warunki wygranej.
     */
    public WinConditionResult checkWinConditions(Game game) {
        int aliveMafia = gamePlayerRepository.countByGameAndAssignedRoleAndIsAlive(
                game, GameRole.MAFIA, true);
        int totalAlive = gamePlayerRepository.countByGameAndIsAlive(game, true);
        int aliveCitizens = totalAlive - aliveMafia;

        log.debug("Win condition check: {} mafia, {} citizens alive", aliveMafia, aliveCitizens);

        // Obywatele wygrywają gdy wszystkie mafie są wyeliminowane
        if (aliveMafia == 0) {
            return new WinConditionResult(true, "CITIZENS",
                    "All mafia members have been eliminated!");
        }

        // Mafia wygrywa gdy liczba mafii >= liczba obywateli
        if (aliveMafia >= aliveCitizens) {
            return new WinConditionResult(true, "MAFIA",
                    "Mafia has taken control of the town!");
        }

        return new WinConditionResult(false, null, null);
    }

    /**
     * Obsługuje zakończenie gry.
     */
    @Transactional
    public void handleGameEnd(Game game, WinConditionResult winResult) {
        log.info("=== GAME ENDING === Winner: {}", winResult.getWinner());

        // Przejdź do stanu GAME_OVER
        phaseStateMachine.transition(game, GamePhaseEvent.WIN_CONDITION_MET);

        game.setStatus(GameStatus.FINISHED);
        game.setEndedAt(LocalDateTime.now());
        gameRepository.save(game);

        // Pobierz graczy dla zdarzenia
        List<GamePlayer> players = gamePlayerRepository.findAllByGameId(game.getId());

        // Publikuj zdarzenie zakończenia gry do RabbitMQ
        eventPublisher.publishGameEnded(game, winResult.getWinner(), winResult.message(), players);

        // Zaktualizuj status pokoju
        GameRoom room = game.getRoom();
        room.setGameRoomStatus(GameRoomStatus.OPEN);
        gameRoomRepository.save(room);

        // Broadcast zakończenie gry
        broadcastGameOver(game, winResult.getWinner());

        // Broadcast zmianę statusu pokoju
        broadcastRoomStatusChange(room);
    }

    /**
     * Planuje przejście do następnej fazy po opóźnieniu.
     */
    private void scheduleNextPhase(Game game, VotingResult result) {
        // W prostej implementacji - od razu przechodzimy do następnej fazy
        // W przyszłości można dodać opóźnienie (np. 5 sekund na wyświetlenie wyniku)
        log.info("Scheduling next phase for game {}", game.getId());

        // Bezpośrednie przejście
        startNextPhase(game);
    }

    // ==================== WEBSOCKET BROADCASTS ====================

    private void broadcastVotingResult(Game game, VotingResult result) {
        try {
            String roomCode = game.getRoom().getRoomCode();

            // Publikuj zdarzenie eliminacji do RabbitMQ (jeśli ktoś został wyeliminowany)
            if (result.getEliminatedUser() != null) {
                GamePlayer eliminatedPlayer = gamePlayerRepository
                        .findByGameAndUser(game, result.getEliminatedUser())
                        .orElse(null);

                String role = eliminatedPlayer != null
                        ? eliminatedPlayer.getAssignedRole().name()
                        : "UNKNOWN";

                // Pobierz liczbę głosów z topVotedPlayers
                int votesReceived = result.getTopVotedPlayers().stream()
                        .filter(vr -> vr.getTargetUser().getId().equals(result.getEliminatedUser().getId()))
                        .findFirst()
                        .map(vr -> vr.getVoteCount())
                        .orElse(0);

                eventPublisher.publishPlayerEliminated(
                        game,
                        result.getEliminatedUser(),
                        role,
                        game.getCurrentPhase().name(),
                        votesReceived
                );
            }

            Map<String, Object> payload = new HashMap<>();
            payload.put("type", "voting_result");
            payload.put("gameId", game.getId());
            payload.put("phase", game.getCurrentPhase().name());
            payload.put("dayNumber", game.getCurrentDayNumber());
            payload.put("eliminated", result.getEliminatedUser() != null
                    ? result.getEliminatedUser().getUsername() : null);
            payload.put("eliminatedUserId", result.getEliminatedUser() != null
                    ? result.getEliminatedUser().getId() : null);
            payload.put("isTie", result.isTie());
            payload.put("resultType", result.getResultType().name());

            String topic = "/topic/game/" + roomCode + "/phase/result";
            messagingTemplate.convertAndSend(topic, (Object) payload);

            log.info("Broadcast voting result to {}", topic);
        } catch (Exception e) {
            log.error("Error broadcasting voting result", e);
        }
    }

    private void broadcastPhaseChange(Game game, GamePhaseState newState) {
        try {
            String roomCode = game.getRoom().getRoomCode();

            String previousPhase = game.getCurrentPhase() != null ? game.getCurrentPhase().name() : "NONE";
            String newPhaseName = phaseStateMachine.mapToGamePhase(newState).name();
            eventPublisher.publishPhaseChanged(game, previousPhase, newPhaseName);

            Map<String, Object> payload = new HashMap<>();
            payload.put("type", "phase_change");
            payload.put("gameId", game.getId());
            payload.put("phase", phaseStateMachine.mapToGamePhase(newState).name());
            payload.put("phaseState", newState.name());
            payload.put("dayNumber", game.getCurrentDayNumber());
            payload.put("isVotingPhase", phaseStateMachine.isVotingPhase(game));

            String topic = "/topic/game/" + roomCode + "/phase/change";
            messagingTemplate.convertAndSend(topic, (Object) payload);

            log.info("Broadcast phase change to {}: {}", topic, newState);
        } catch (Exception e) {
            log.error("Error broadcasting phase change", e);
        }
    }

    private void broadcastGameOver(Game game, String winner) {
        try {
            String roomCode = game.getRoom().getRoomCode();

            // Pobierz wszystkich graczy z ich rolami
            List<GamePlayer> players = gamePlayerRepository.findAllByGameId(game.getId());
            List<Map<String, Object>> playerData = players.stream()
                    .map(p -> {
                        Map<String, Object> data = new HashMap<>();
                        data.put("userId", p.getUser().getId());
                        data.put("username", p.getUser().getUsername());
                        data.put("role", p.getAssignedRole().name());
                        data.put("isAlive", p.isAlive());
                        return data;
                    })
                    .collect(Collectors.toList());

            Map<String, Object> payload = new HashMap<>();
            payload.put("type", "game_over");
            payload.put("gameId", game.getId());
            payload.put("winner", winner);
            payload.put("totalDays", game.getCurrentDayNumber());
            payload.put("endedAt", game.getEndedAt());
            payload.put("players", playerData);

            String topic = "/topic/game/" + roomCode + "/gameOver";
            messagingTemplate.convertAndSend(topic, (Object) payload);

            log.info("Broadcast game over to {}: winner={}", topic, winner);
        } catch (Exception e) {
            log.error("Error broadcasting game over", e);
        }
    }

    private void broadcastRoomStatusChange(GameRoom room) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("type", "room_status_changed");
            payload.put("roomCode", room.getRoomCode());
            payload.put("status", room.getGameRoomStatus().name());
            payload.put("message", "Game ended. Room is now open for a new game.");

            String topic = "/topic/game/" + room.getRoomCode() + "/updated";
            messagingTemplate.convertAndSend(topic, (Object) payload);

            log.info("Broadcast room status change to {}", topic);
        } catch (Exception e) {
            log.error("Error broadcasting room status change", e);
        }
    }

    // ==================== INNER CLASSES ====================

    /**
     * Wynik sprawdzenia warunków wygranej.
     */
    public record WinConditionResult(
            boolean hasWinner,
            String winner,
            String message
    ) {
        public boolean hasWinner() {
            return hasWinner;
        }

        public String getWinner() {
            return winner;
        }
    }
}
