package com.mafia.gameservice.services;

import static org.assertj.core.api.Assertions.assertThat;

import com.mafia.gameservice.models.User;
import com.mafia.gameservice.models.VoteResult;
import com.mafia.gameservice.support.GameTestFixtures;
import java.util.List;
import org.junit.jupiter.api.Test;

class VotingResultTest {

    @Test
    void eliminationFactoryBuildsExpectedResult() {
        User eliminated = GameTestFixtures.user("victim");
        VoteResult voteResult = new VoteResult();
        voteResult.setTargetUser(eliminated);
        voteResult.setVoteCount(3);

        VotingResult result = VotingResult.elimination(eliminated, List.of(voteResult));

        assertThat(result.hasElimination()).isTrue();
        assertThat(result.isTie()).isFalse();
        assertThat(result.getResultType()).isEqualTo(VotingResult.ResultType.ELIMINATION);
        assertThat(result.getDescription()).contains("victim");
    }

    @Test
    void tieFactoryIndicatesNoElimination() {
        VotingResult result = VotingResult.tie(List.of(new VoteResult(), new VoteResult()));

        assertThat(result.hasElimination()).isFalse();
        assertThat(result.isTie()).isTrue();
        assertThat(result.getResultType()).isEqualTo(VotingResult.ResultType.TIE_NO_ELIMINATION);
        assertThat(result.getDescription()).contains("Tie");
    }

    @Test
    void tieRandomEliminationFactory() {
        User eliminated = GameTestFixtures.user("unlucky");
        VotingResult result = VotingResult.tieRandomElimination(eliminated, List.of(new VoteResult()));

        assertThat(result.hasElimination()).isTrue();
        assertThat(result.isTie()).isTrue();
        assertThat(result.getResultType()).isEqualTo(VotingResult.ResultType.TIE_RANDOM_ELIMINATION);
    }

    @Test
    void noEliminationFactory() {
        VotingResult result = VotingResult.noElimination();

        assertThat(result.hasElimination()).isFalse();
        assertThat(result.getResultType()).isEqualTo(VotingResult.ResultType.NO_ELIMINATION);
        assertThat(result.getDescription()).contains("No elimination");
    }
}
