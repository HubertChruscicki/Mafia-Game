package com.mafia.gameservice.dto.gameRoom;

import com.mafia.gameservice.dto.PlayerInRoomResponse;
import com.mafia.gameservice.enums.GameRoomStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Full room details response Used for GET /api/game_rooms/{roomCode} Contains all room information
 * including full player details
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GameRoomInfoResp {
  private UUID id; // Room ID - needed for starting game
  private String roomCode;
  private String name;
  private String hostId;
  private String hostUsername;
  private int maxPlayers;
  private int currentPlayers;
  private GameRoomStatus status;
  private LocalDateTime createdAt;
  private List<String> playerIds; // Kept for backward compatibility
  private List<PlayerInRoomResponse> players; // Full player details
}
