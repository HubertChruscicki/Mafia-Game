package com.mafia.gameservice.repositories;

import com.mafia.gameservice.models.GamePlayer;
import com.mafia.gameservice.enums.GameRole;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PlayerGameStateRepository extends JpaRepository<GamePlayer, UUID> {

    @Query("select gp from GamePlayer gp where gp.game.room.roomCode = :roomCode and gp.user.id = :userId")
    GamePlayer findByRoomCodeAndUserId(@Param("roomCode") String roomCode, @Param("userId") UUID userId);

    @Query("select count(gp) from GamePlayer gp where gp.game.room.roomCode = :roomCode and gp.isAlive = true")
    long countByRoomCodeAndAliveTrue(@Param("roomCode") String roomCode);

    @Query("select gp from GamePlayer gp where gp.game.room.roomCode = :roomCode and gp.isAlive = true")
    List<GamePlayer> findByRoomCodeAndAliveTrue(@Param("roomCode") String roomCode);

    @Query("select gp from GamePlayer gp where gp.game.room.roomCode = :roomCode and gp.isAlive = true and gp.assignedRole = :role")
    List<GamePlayer> findByRoomCodeAndAliveTrueAndRole(@Param("roomCode") String roomCode, @Param("role") GameRole role);
}