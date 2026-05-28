package com.mafia.gameservice.dto.gameroom;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateGameRoomResp {

    private String roomCode;
    private String name;
}
