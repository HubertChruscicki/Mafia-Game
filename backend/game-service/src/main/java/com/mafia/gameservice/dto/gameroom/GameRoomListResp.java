package com.mafia.gameservice.dto.gameroom;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GameRoomListResp {

    private List<GameRoomInfoResp> rooms;
    private int totalCount;
    private boolean isEmpty;

    public static GameRoomListResp of(List<GameRoomInfoResp> rooms) {
        return new GameRoomListResp(
                rooms,
                rooms != null ? rooms.size() : 0,
                rooms == null || rooms.isEmpty());
    }
}
