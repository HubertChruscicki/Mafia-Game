package com.mafia.gameservice.exceptions;

public class VotingSessionNotFoundException extends RuntimeException {
    public VotingSessionNotFoundException(String message) {
        super(message);
    }
}
