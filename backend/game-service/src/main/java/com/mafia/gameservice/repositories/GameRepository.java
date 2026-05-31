package com.mafia.gameservice.repositories;

import com.mafia.gameservice.models.Game;
import com.mafia.gameservice.enums.GameStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GameRepository extends JpaRepository<Game, UUID> {
  Optional<Game> findById(UUID id);
  List<Game> findByRoom_IdAndStatus(UUID roomId, GameStatus status);
  Optional<Game> findFirstByRoom_IdOrderByCreatedAtDesc(UUID roomId);
}
