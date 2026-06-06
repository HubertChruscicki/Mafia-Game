package com.mafia.gameservice.dto;

import com.mafia.gameservice.enums.GamePhase;
import com.mafia.gameservice.enums.GameRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VoteRequest {

    @NotNull(message = "gameId is required")
    private UUID gameId;

    @NotBlank(message = "targetUsername is required")
    private String targetUsername;

    @NotNull(message = "voterUsername is required")
    private String voterUsername;

    @NotNull(message = "gamePhase is required")
    private GamePhase gamePhase;

    @NotNull(message = "voterRole is required")
    private GameRole voterRole;

    @NotNull(message = "dayNumber is required")
    private Integer dayNumber;

    private LocalDateTime votedAt;

}
