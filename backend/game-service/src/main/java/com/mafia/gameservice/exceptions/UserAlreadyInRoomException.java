package com.mafia.gameservice.exceptions;

public class UserAlreadyInRoomException extends RuntimeException
{
    public UserAlreadyInRoomException(String message) { super(message); }
}