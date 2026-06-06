package com.mafia.gameservice.dto.gameRoom;

import com.mafia.gameservice.dto.PlayerInRoomResponse;
import com.mafia.gameservice.enums.GameRoomStatus;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * WebSocket update indication for real-time room updates
 * Contains player IDs and full player details for immediate UI updates
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GameRoomUpdateInd {
  private String roomCode;
  private int currentPlayers;
  private GameRoomStatus status;
  private List<String> playerIds; // List of user UUIDs (for backward compatibility)
  private List<PlayerInRoomResponse> players; // Full player details
}
