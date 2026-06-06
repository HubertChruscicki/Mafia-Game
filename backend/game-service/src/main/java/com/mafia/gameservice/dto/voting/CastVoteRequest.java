package com.mafia.gameservice.dto.voting;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CastVoteRequest {

    @NotNull(message = "Voting session ID is required")
    private UUID votingSessionId;

    @NotNull(message = "Target user ID is required")
    private UUID targetUserId;
}
