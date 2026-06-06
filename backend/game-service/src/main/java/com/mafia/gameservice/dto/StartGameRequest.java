package com.mafia.gameservice.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StartGameRequest {

    @NotBlank(message = "Room code is required")
    private String roomCode;

    @Min(value = 1, message = "At least 1 mafia player required")
    @Max(value = 5, message = "Maximum 5 mafia players allowed")
    private int mafiaCount = 1;

    @Min(value = 30, message = "Minimum discussion time is 30 seconds")
    @Max(value = 600, message = "Maximum discussion time is 600 seconds")
    private int discussionTimeSeconds = 120;
}
