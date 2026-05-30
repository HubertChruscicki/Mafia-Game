package com.mafia.gameservice.repositories;

import com.mafia.gameservice.models.GameRoom;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GameRoomRepository extends JpaRepository<GameRoom, UUID> {
  boolean existsByRoomCode(String roomCode);

  Optional<GameRoom> findByRoomCode(String roomCode);

  List<GameRoom> findByNameContainingIgnoreCase(String name);
}
