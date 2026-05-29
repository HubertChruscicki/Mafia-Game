package com.mafia.gameservice.services;

import java.util.List;

import com.mafia.gameservice.models.User;
import com.mafia.gameservice.models.VoteResult;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Value Object reprezentujący wynik głosowania
 * Immutable - bezpieczny w środowisku wielowątkowym
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class VotingResult {

    /** Użytkownik który został wyeliminowany (null jeśli nikt) */
    private final User eliminatedUser;

    /** Czy był remis */
    private final boolean isTie;

    /** Lista graczy z największą liczbą głosów (w przypadku remisu) */
    private final List<VoteResult> topVotedPlayers;

    /** Typ wyniku */
    private final ResultType resultType;

    public enum ResultType {
        /** Gracz został wyeliminowany */
        ELIMINATION,

        /** Remis - nikt nie odpada (dla DAY_VOTE) */
        TIE_NO_ELIMINATION,

        /** Remis - losowo wybrano gracza (dla NIGHT_VOTE) */
        TIE_RANDOM_ELIMINATION,

        /** Brak eliminacji - nikt nie głosował lub wszyscy się wstrzymali */
        NO_ELIMINATION
    }

    /**
     * Opis wyniku (dla logowania)
     */
    public String getDescription() {
        return switch (resultType) {
            case ELIMINATION -> "Player " + eliminatedUser.getUsername() + " was eliminated";
            case TIE_NO_ELIMINATION -> "Tie - no elimination (" + topVotedPlayers.size() + " players)";
            case TIE_RANDOM_ELIMINATION ->
                    "Tie - random elimination: " + eliminatedUser.getUsername();
            case NO_ELIMINATION -> "No elimination - no votes or all abstained";
        };
    }

    public boolean hasElimination() {
        return eliminatedUser != null;
    }

    // ==================== FACTORY METHODS ====================

    public static VotingResult elimination(User eliminated, List<VoteResult> topVoted) {
        return new VotingResult(eliminated, false, topVoted, ResultType.ELIMINATION);
    }

    public static VotingResult tie(List<VoteResult> topVoted) {
        return new VotingResult(null, true, topVoted, ResultType.TIE_NO_ELIMINATION);
    }

    public static VotingResult tieRandomElimination(User eliminated, List<VoteResult> topVoted) {
        return new VotingResult(eliminated, true, topVoted, ResultType.TIE_RANDOM_ELIMINATION);
    }

    public static VotingResult noElimination() {
        return new VotingResult(null, false, List.of(), ResultType.NO_ELIMINATION);
    }
}
