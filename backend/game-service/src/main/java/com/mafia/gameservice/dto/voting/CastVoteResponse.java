package com.mafia.gameservice.dto.voting;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CastVoteResponse {

    private boolean success;
    private String message;
    private UUID votedFor;

    public static CastVoteResponse success(UUID votedFor) {
        return new CastVoteResponse(true, "Vote cast successfully", votedFor);
    }

    public static CastVoteResponse error(String message) {
        return new CastVoteResponse(false, message, null);
    }
}
