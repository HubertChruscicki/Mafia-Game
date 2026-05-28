package com.mafia.gameservice.repositories;

import com.mafia.gameservice.enums.GameStatus;
import com.mafia.gameservice.models.Game;
import com.mafia.gameservice.models.GameRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GameRepository extends JpaRepository<Game, UUID> {

    List<Game> findByRoomAndStatus(GameRoom room, GameStatus status);

    List<Game> findByRoom_IdAndStatus(UUID roomId, GameStatus status);

    Optional<Game> findFirstByRoomAndStatusOrderByCreatedAtDesc(GameRoom room, GameStatus status);
}
