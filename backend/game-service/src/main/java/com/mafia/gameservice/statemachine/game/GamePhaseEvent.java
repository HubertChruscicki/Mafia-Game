package com.mafia.gameservice.statemachine.game;

/**
 * Zdarzenia powodujące przejścia między fazami gry.
 */
public enum GamePhaseEvent {
    /**
     * Gra została rozpoczęta - przejście do pierwszej fazy (NOC)
     */
    GAME_STARTED,

    /**
     * Głosowanie zakończone (wszyscy zagłosowali lub czas minął)
     */
    VOTING_COMPLETED,

    /**
     * Wynik głosowania został przetworzony, gracz wyeliminowany lub remis
     */
    RESULT_PROCESSED,

    /**
     * Warunek wygranej został spełniony (mafia lub obywatele wygrali)
     */
    WIN_CONDITION_MET
}
