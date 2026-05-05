package com.mafia.gameservice.statemachine;
/**
 * Listener dla zmian stanu w maszynie stanów.
 * Pozwala na reagowanie na przejścia między stanami.
 *
 * @param <S> Typ enum reprezentujący stany
 * @param <C> Typ kontekstu
 */
public interface StateChangeListener<S extends Enum<S>, C> {

    /**
     * Wywoływane przed zmianą stanu
     */
    default void beforeStateChange(S oldState, S newState, C context) {}

    /**
     * Wywoływane po zmianie stanu
     */
    void onStateChange(S oldState, S newState, C context);

    /**
     * Wywoływane gdy przejście nie jest dozwolone
     */
    default void onInvalidTransition(S currentState, Object event, C context) {}
}
