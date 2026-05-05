package com.mafia.gameservice.statemachine;

/**
 * Interfejs bazowy dla maszyny stanów.
 * Definiuje kontrakt dla wszystkich maszyn stanów w aplikacji.
 *
 * @param <S> Typ enum reprezentujący stany
 * @param <E> Typ enum reprezentujący zdarzenia
 * @param <C> Typ kontekstu (np. Game, VotingSession)
 */
public interface StateMachine<S extends Enum<S>, E extends Enum<E>, C> {

    /**
     * Pobiera aktualny stan dla danego kontekstu
     */
    S getCurrentState(C context);

    /**
     * Sprawdza czy przejście jest dozwolone
     */
    boolean canTransition(C context, E event);

    /**
     * Wykonuje przejście stanu
     * @return nowy stan po przejściu
     * @throws IllegalStateException jeśli przejście nie jest dozwolone
     */
    S transition(C context, E event);

    /**
     * Dodaje listener zmian stanu
     */
    void addListener(StateChangeListener<S, C> listener);

    /**
     * Usuwa listener
     */
    void removeListener(StateChangeListener<S, C> listener);
}
