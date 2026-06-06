package com.mafia.gameservice.repositories;

import com.mafia.gameservice.models.GameRoundSummary;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GameRoundSummaryRepository extends JpaRepository<GameRoundSummary, UUID> {}
