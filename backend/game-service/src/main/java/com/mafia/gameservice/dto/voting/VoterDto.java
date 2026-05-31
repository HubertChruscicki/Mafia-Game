package com.mafia.gameservice.dto.voting;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO reprezentujący pojedynczego głosującego
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VoterDto {
  private UUID voterId;
  private String voterUsername;
  private LocalDateTime votedAt;
}
