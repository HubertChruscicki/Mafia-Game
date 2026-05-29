package com.mafia.gameservice.statemachine.game;
import com.mafia.gameservice.enums.GamePhase;
import com.mafia.gameservice.models.Game;
import com.mafia.gameservice.statemachine.AbstractStateMachine;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Maszyna stanów dla faz gry Mafia.
 *
 * Zarządza przejściami między fazami:
 * - NIGHT_VOTING: Mafia głosuje kogo zabić
 * - NIGHT_RESULT: Ogłoszenie ofiary nocy
 * - DAY_VOTING: Wszyscy głosują kogo wyeliminować
 * - DAY_RESULT: Ogłoszenie wyeliminowanego
 * - GAME_OVER: Koniec gry
 *
 * Diagram przejść:
 *
 *                    ┌─────────────────────────────────────────┐
 *                    │                                         │
 *                    ▼                                         │
 *  [START] ──► NIGHT_VOTING ──► NIGHT_RESULT ──► DAY_VOTING ──┤
 *                    ▲                               │         │
 *                    │                               ▼         │
 *                    └────────────── DAY_RESULT ─────┘         │
 *                                         │                    │
 *                                         │ (win condition)    │
 *                                         ▼                    │
 *                                    GAME_OVER ◄───────────────┘
 *                                                (win condition)
 */
@Component
@Slf4j
public class GamePhaseStateMachine extends AbstractStateMachine<GamePhaseState, GamePhaseEvent, Game> {

    @PostConstruct
    public void init() {
        defineTransitions();
        log.info("GamePhaseStateMachine initialized with {} states", transitions.size());
    }

    private void defineTransitions() {
        // Z NIGHT_VOTING
        defineTransition(GamePhaseState.NIGHT_VOTING, GamePhaseEvent.VOTING_COMPLETED, GamePhaseState.NIGHT_RESULT);
        defineTransition(GamePhaseState.NIGHT_VOTING, GamePhaseEvent.WIN_CONDITION_MET, GamePhaseState.GAME_OVER);

        // Z NIGHT_RESULT
        defineTransition(GamePhaseState.NIGHT_RESULT, GamePhaseEvent.RESULT_PROCESSED, GamePhaseState.DAY_VOTING);
        defineTransition(GamePhaseState.NIGHT_RESULT, GamePhaseEvent.WIN_CONDITION_MET, GamePhaseState.GAME_OVER);

        // Z DAY_VOTING
        defineTransition(GamePhaseState.DAY_VOTING, GamePhaseEvent.VOTING_COMPLETED, GamePhaseState.DAY_RESULT);
        defineTransition(GamePhaseState.DAY_VOTING, GamePhaseEvent.WIN_CONDITION_MET, GamePhaseState.GAME_OVER);

        // Z DAY_RESULT
        defineTransition(GamePhaseState.DAY_RESULT, GamePhaseEvent.RESULT_PROCESSED, GamePhaseState.NIGHT_VOTING);
        defineTransition(GamePhaseState.DAY_RESULT, GamePhaseEvent.WIN_CONDITION_MET, GamePhaseState.GAME_OVER);
    }

    @Override
    protected Class<GamePhaseState> getStateClass() {
        return GamePhaseState.class;
    }

    @Override
    protected Class<GamePhaseEvent> getEventClass() {
        return GamePhaseEvent.class;
    }

    @Override
    public GamePhaseState getCurrentState(Game game) {
        if (game == null || game.getCurrentPhase() == null) {
            return null;
        }
        return mapFromGamePhase(game.getCurrentPhase());
    }

    @Override
    protected void setState(Game game, GamePhaseState newState) {
        GamePhase gamePhase = mapToGamePhase(newState);
        game.setCurrentPhase(gamePhase);
        log.debug("Game {} phase set to {}", game.getId(), gamePhase);
    }

    @Override
    protected String getMachineName() {
        return "GamePhase";
    }

    @Override
    protected String getContextId(Game game) {
        return game != null ? "Game:" + game.getId() : "null";
    }

    /**
     * Mapuje GamePhase (z bazy danych) na GamePhaseState (maszyna stanów)
     */
    public GamePhaseState mapFromGamePhase(GamePhase phase) {
        return switch (phase) {
            case NIGHT_VOTE -> GamePhaseState.NIGHT_VOTING;
            case NIGHT_RESULT -> GamePhaseState.NIGHT_RESULT;
            case DAY_VOTE -> GamePhaseState.DAY_VOTING;
            case DAY_RESULT -> GamePhaseState.DAY_RESULT;
            case DAY_DISCUSSION -> GamePhaseState.DAY_VOTING; // Traktujemy dyskusję jako część głosowania
            case GAME_OVER -> GamePhaseState.GAME_OVER;
        };
    }

    /**
     * Mapuje GamePhaseState (maszyna stanów) na GamePhase (baza danych)
     */
    public GamePhase mapToGamePhase(GamePhaseState state) {
        return switch (state) {
            case NIGHT_VOTING -> GamePhase.NIGHT_VOTE;
            case NIGHT_RESULT -> GamePhase.NIGHT_RESULT;
            case DAY_VOTING -> GamePhase.DAY_VOTE;
            case DAY_RESULT -> GamePhase.DAY_RESULT;
            case GAME_OVER -> GamePhase.GAME_OVER;
        };
    }

    /**
     * Sprawdza czy aktualny stan to faza głosowania
     */
    public boolean isVotingPhase(Game game) {
        GamePhaseState state = getCurrentState(game);
        return state == GamePhaseState.NIGHT_VOTING || state == GamePhaseState.DAY_VOTING;
    }

    /**
     * Sprawdza czy aktualny stan to faza wyników
     */
    public boolean isResultPhase(Game game) {
        GamePhaseState state = getCurrentState(game);
        return state == GamePhaseState.NIGHT_RESULT || state == GamePhaseState.DAY_RESULT;
    }

    /**
     * Sprawdza czy gra jest zakończona
     */
    public boolean isGameOver(Game game) {
        return getCurrentState(game) == GamePhaseState.GAME_OVER;
    }
}
