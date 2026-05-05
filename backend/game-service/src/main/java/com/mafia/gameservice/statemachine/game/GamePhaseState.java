package com.mafia.gameservice.statemachine.game;

/**
 * Stany faz gry Mafia.
 *
 * Przepływ gry:
 * NIGHT_VOTING → NIGHT_RESULT → DAY_VOTING → DAY_RESULT → NIGHT_VOTING → ...
 *
 * W każdym momencie może nastąpić przejście do GAME_OVER gdy warunki wygranej są spełnione.
 */
public enum GamePhaseState {
    /**
     * Faza nocnego głosowania - mafia wybiera ofiarę
     */
    NIGHT_VOTING,

    /**
     * Wynik nocnego głosowania - ogłoszenie kto został zabity
     */
    NIGHT_RESULT,

    /**
     * Faza dziennego głosowania - wszyscy głosują kogo wyeliminować
     */
    DAY_VOTING,

    /**
     * Wynik dziennego głosowania - ogłoszenie kto został wyeliminowany
     */
    DAY_RESULT,

    /**
     * Gra zakończona - jeden z zespołów wygrał
     */
    GAME_OVER
}
