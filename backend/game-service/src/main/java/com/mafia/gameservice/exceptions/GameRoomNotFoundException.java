package com.mafia.gameservice.exceptions;

public class GameRoomNotFoundException extends RuntimeException
{
    public GameRoomNotFoundException(String message) { super(message); }
}