package com.mafia.gameservice.dto.gameroom;

import com.mafia.gameservice.dto.PlayerInRoomResponse;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GameRoomInfoResp {

    private UUID id;
    private String roomCode;
    private String name;
    private UUID hostId;
    private String hostUsername;
    private int maxPlayers;
    private int currentPlayers;
    private String status;
    private int mafiaCount;
    private int discussionTimeSeconds;
    private LocalDateTime createdAt;
    private List<PlayerInRoomResponse> players;
}
