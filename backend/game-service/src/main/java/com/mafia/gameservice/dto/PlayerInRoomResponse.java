package com.mafia.gameservice.dto;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PlayerInRoomResponse {

    private UUID playerId;
    private UUID userId;
    private String username;
    private boolean isHost;
    private LocalDateTime joinedAt;

    public PlayerInRoomResponse() {}

    public PlayerInRoomResponse(UUID playerId, UUID userId, String username, boolean isHost,
                                LocalDateTime joinedAt) {
        this.playerId = playerId;
        this.userId = userId;
        this.username = username;
        this.isHost = isHost;
        this.joinedAt = joinedAt;
    }
}
