package com.mafia.gameservice.statemachine.game;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mafia.gameservice.enums.GamePhase;
import com.mafia.gameservice.models.Game;
import com.mafia.gameservice.support.GameTestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class GamePhaseStateMachineTest {

    private GamePhaseStateMachine stateMachine;

    @BeforeEach
    void setUp() {
        stateMachine = new GamePhaseStateMachine();
        stateMachine.init();
    }

    @Test
    void nightVotingCompletesToNightResult() {
        Game game = gameInPhase(GamePhase.NIGHT_VOTE);

        stateMachine.transition(game, GamePhaseEvent.VOTING_COMPLETED);

        assertThat(game.getCurrentPhase()).isEqualTo(GamePhase.NIGHT_RESULT);
    }

    @Test
    void nightResultProcessesToDayVoting() {
        Game game = gameInPhase(GamePhase.NIGHT_RESULT);

        stateMachine.transition(game, GamePhaseEvent.RESULT_PROCESSED);

        assertThat(game.getCurrentPhase()).isEqualTo(GamePhase.DAY_VOTE);
    }

    @Test
    void dayVotingCompletesToDayResult() {
        Game game = gameInPhase(GamePhase.DAY_VOTE);

        stateMachine.transition(game, GamePhaseEvent.VOTING_COMPLETED);

        assertThat(game.getCurrentPhase()).isEqualTo(GamePhase.DAY_RESULT);
    }

    @Test
    void dayResultProcessesBackToNightVoting() {
        Game game = gameInPhase(GamePhase.DAY_RESULT);

        stateMachine.transition(game, GamePhaseEvent.RESULT_PROCESSED);

        assertThat(game.getCurrentPhase()).isEqualTo(GamePhase.NIGHT_VOTE);
    }

    @ParameterizedTest
    @EnumSource(value = GamePhase.class, names = {"NIGHT_VOTE", "NIGHT_RESULT", "DAY_VOTE", "DAY_RESULT"})
    void winConditionEndsGameFromAnyActivePhase(GamePhase phase) {
        Game game = gameInPhase(phase);

        stateMachine.transition(game, GamePhaseEvent.WIN_CONDITION_MET);

        assertThat(game.getCurrentPhase()).isEqualTo(GamePhase.GAME_OVER);
        assertThat(stateMachine.isGameOver(game)).isTrue();
    }

    @Test
    void invalidTransitionThrows() {
        Game game = gameInPhase(GamePhase.GAME_OVER);

        assertThatThrownBy(() -> stateMachine.transition(game, GamePhaseEvent.VOTING_COMPLETED))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void mapsDayDiscussionToDayVotingState() {
        Game game = gameInPhase(GamePhase.DAY_DISCUSSION);

        assertThat(stateMachine.getCurrentState(game)).isEqualTo(GamePhaseState.DAY_VOTING);
        assertThat(stateMachine.isVotingPhase(game)).isTrue();
    }

    @Test
    void detectsResultAndVotingPhases() {
        assertThat(stateMachine.isVotingPhase(gameInPhase(GamePhase.NIGHT_VOTE))).isTrue();
        assertThat(stateMachine.isVotingPhase(gameInPhase(GamePhase.DAY_VOTE))).isTrue();
        assertThat(stateMachine.isResultPhase(gameInPhase(GamePhase.NIGHT_RESULT))).isTrue();
        assertThat(stateMachine.isResultPhase(gameInPhase(GamePhase.DAY_RESULT))).isTrue();
        assertThat(stateMachine.isVotingPhase(gameInPhase(GamePhase.GAME_OVER))).isFalse();
    }

    @Test
    void canTransitionReflectsDefinedEdges() {
        Game game = gameInPhase(GamePhase.NIGHT_VOTE);

        assertThat(stateMachine.canTransition(game, GamePhaseEvent.VOTING_COMPLETED)).isTrue();
        assertThat(stateMachine.canTransition(game, GamePhaseEvent.RESULT_PROCESSED)).isFalse();
    }

    @Test
    void roundTripPhaseMapping() {
        for (GamePhaseState state : GamePhaseState.values()) {
            if (state == GamePhaseState.GAME_OVER) {
                continue;
            }
            assertThat(stateMachine.mapFromGamePhase(stateMachine.mapToGamePhase(state))).isEqualTo(state);
        }
    }

    private Game gameInPhase(GamePhase phase) {
        Game game = GameTestFixtures.activeGame(GameTestFixtures.openRoom(GameTestFixtures.user("host")));
        game.setCurrentPhase(phase);
        return game;
    }
}
