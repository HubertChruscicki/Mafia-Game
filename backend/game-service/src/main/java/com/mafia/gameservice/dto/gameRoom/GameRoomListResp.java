package com.mafia.gameservice.dto.gameRoom;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response containing list of game rooms")
public class GameRoomListResp {

  @Schema(description = "List of game rooms", example = "[]")
  private List<GameRoomInfoResp> rooms;

  @Schema(description = "Total count of rooms returned", example = "5")
  private int totalCount;

  @Schema(description = "Whether the list is empty", example = "false")
  private boolean isEmpty;

  /**
   * Factory method to create response from list of rooms
   * @param rooms list of game room info responses
   * @return GameRoomListResp with populated data
   */
  public static GameRoomListResp of(List<GameRoomInfoResp> rooms) {
    return new GameRoomListResp(
        rooms,
        rooms != null ? rooms.size() : 0,
        rooms == null || rooms.isEmpty()
    );
  }
}
