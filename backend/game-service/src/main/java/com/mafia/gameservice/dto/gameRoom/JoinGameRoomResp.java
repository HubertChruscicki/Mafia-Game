package com.mafia.gameservice.dto.gameRoom;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JoinGameRoomResp {
  private String roomCode;
  private String roomName;
  private String hostUsername;
  private int maxPlayers;
  private int currentPlayers;
  private String status;
}
