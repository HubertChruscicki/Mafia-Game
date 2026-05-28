package com.mafia.gameservice.dto.voting;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VoteResultDto {

    private UUID targetUserId;
    private String targetUsername;
    private int voteCount;
}
