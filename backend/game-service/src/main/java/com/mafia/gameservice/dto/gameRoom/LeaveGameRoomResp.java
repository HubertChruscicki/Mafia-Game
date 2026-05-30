package com.mafia.gameservice.dto.gameRoom;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LeaveGameRoomResp {
  private String roomCode;
  private boolean roomDeleted; // true if host left and room was deleted
  private int currentPlayers;  // players remaining after leave, 0 if deleted
}
