package com.mafia.gameservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GamePlayerDto {

    private UUID userId;
    private String username;
    private String role; // null if hidden (only show to owner or after game ends)

    @JsonProperty("isAlive")
    private boolean isAlive;

    @JsonProperty("isHost")
    private boolean isHost;
}
