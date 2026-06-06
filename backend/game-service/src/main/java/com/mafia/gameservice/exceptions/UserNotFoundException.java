package com.mafia.gameservice.exceptions;

public class UserNotFoundException extends RuntimeException
{
    public UserNotFoundException(String message) { super(message); }
}
