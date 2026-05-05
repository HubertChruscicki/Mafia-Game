package com.mafia.gameservice.statemachine;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * Abstrakcyjna implementacja maszyny stanów.
 * Dostarcza wspólną logikę dla wszystkich maszyn stanów.
 *
 * @param <S> Typ enum reprezentujący stany
 * @param <E> Typ enum reprezentujący zdarzenia
 * @param <C> Typ kontekstu
 */
@Slf4j
public abstract class AbstractStateMachine<S extends Enum<S>, E extends Enum<E>, C>
        implements StateMachine<S, E, C> {

    protected final Map<S, Map<E, S>> transitions = new EnumMap<>(getStateClass());
    protected final List<StateChangeListener<S, C>> listeners = new ArrayList<>();

    /**
     * Zwraca klasę enum stanów - musi być zaimplementowane przez podklasy
     */
    protected abstract Class<S> getStateClass();

    /**
     * Pobiera aktualny stan z kontekstu - musi być zaimplementowane przez podklasy
     */
    @Override
    public abstract S getCurrentState(C context);

    /**
     * Ustawia nowy stan w kontekście - musi być zaimplementowane przez podklasy
     */
    protected abstract void setState(C context, S newState);

    /**
     * Zwraca nazwę maszyny stanów dla logowania
     */
    protected abstract String getMachineName();

    @Override
    public boolean canTransition(C context, E event) {
        S currentState = getCurrentState(context);
        Map<E, S> stateTransitions = transitions.get(currentState);
        return stateTransitions != null && stateTransitions.containsKey(event);
    }

    @Override
    public S transition(C context, E event) {
        S currentState = getCurrentState(context);
        Map<E, S> stateTransitions = transitions.get(currentState);

        if (stateTransitions == null || !stateTransitions.containsKey(event)) {
            String errorMsg = "[%s] Invalid transition: %s + %s (context: %s)".formatted(
                    getMachineName(), currentState, event, context);
            log.error(errorMsg);

            // Powiadom listenery o nieprawidłowym przejściu
            for (StateChangeListener<S, C> listener : listeners) {
                listener.onInvalidTransition(currentState, event, context);
            }

            throw new IllegalStateException(errorMsg);
        }

        S newState = stateTransitions.get(event);

        log.info("[{}] Transition: {} --({})-> {} (context: {})",
                getMachineName(), currentState, event, newState, getContextId(context));

        // Powiadom listenery przed zmianą
        for (StateChangeListener<S, C> listener : listeners) {
            listener.beforeStateChange(currentState, newState, context);
        }

        // Wykonaj zmianę stanu
        setState(context, newState);

        // Powiadom listenery po zmianie
        for (StateChangeListener<S, C> listener : listeners) {
            listener.onStateChange(currentState, newState, context);
        }

        return newState;
    }

    /**
     * Zwraca identyfikator kontekstu dla logowania
     */
    protected String getContextId(C context) {
        return context != null ? context.toString() : "null";
    }

    @Override
    public void addListener(StateChangeListener<S, C> listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    @Override
    public void removeListener(StateChangeListener<S, C> listener) {
        listeners.remove(listener);
    }

    /**
     * Pomocnicza metoda do definiowania przejść
     */
    protected void defineTransition(S fromState, E event, S toState) {
        transitions.computeIfAbsent(fromState, k -> new EnumMap<>(getEventClass()))
                .put(event, toState);
        log.debug("[{}] Defined transition: {} --({})-> {}",
                getMachineName(), fromState, event, toState);
    }

    /**
     * Zwraca klasę enum zdarzeń - musi być zaimplementowane przez podklasy
     */
    protected abstract Class<E> getEventClass();
}
