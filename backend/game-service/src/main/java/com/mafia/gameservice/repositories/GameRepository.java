package com.mafia.gameservice.repositories;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.mafia.gameservice.enums.GameStatus;
import com.mafia.gameservice.models.Game;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GameRepository extends JpaRepository<Game, UUID> {
}
