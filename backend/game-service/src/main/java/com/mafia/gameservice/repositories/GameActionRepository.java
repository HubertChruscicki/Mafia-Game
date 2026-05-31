package com.mafia.gameservice.repositories;

import com.mafia.gameservice.models.GameAction;
import com.mafia.gameservice.enums.GamePhase;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GameActionRepository extends JpaRepository<GameAction, UUID> {
  List<GameAction> findAllByGameIdAndDayNumberAndPhaseOrderByExecutedAtAsc(UUID gameId, int dayNumber, GamePhase phase);
}
