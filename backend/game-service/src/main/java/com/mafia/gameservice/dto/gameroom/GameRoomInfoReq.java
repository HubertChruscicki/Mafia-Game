package com.mafia.gameservice.dto.gameroom;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GameRoomInfoReq {

    @JsonProperty("roomCode")
    private String roomCode;

    @JsonProperty("userId")
    private UUID userId;

    public boolean isValid() {
        return (roomCode != null && !roomCode.isBlank()) || userId != null;
    }

    public boolean isRoomCodeSearch() {
        return roomCode != null && !roomCode.isBlank();
    }

    public boolean isUserIdSearch() {
        return userId != null;
    }
}
