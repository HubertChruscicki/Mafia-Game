package com.mafia.gameservice.dto.voting;

import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VotingSessionDto {

    private UUID sessionId;
    private String phase;
    private int dayNumber;
    private long remainingSeconds;
    private int totalVoters;
    private int votesReceived;
    private Map<UUID, Integer> voteMap; // targetUserId → vote count
}
