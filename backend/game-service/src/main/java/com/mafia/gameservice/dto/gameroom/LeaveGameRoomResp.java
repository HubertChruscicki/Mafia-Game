package com.mafia.gameservice.dto.gameroom;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LeaveGameRoomResp {

    private boolean success;
    private String message;
}
