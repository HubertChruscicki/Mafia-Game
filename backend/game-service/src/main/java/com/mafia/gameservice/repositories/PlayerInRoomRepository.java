package com.mafia.gameservice.repositories;

import com.mafia.gameservice.models.GameRoom;
import com.mafia.gameservice.models.PlayerInRoom;
import com.mafia.gameservice.models.User;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlayerInRoomRepository extends JpaRepository<PlayerInRoom, UUID> {

    Optional<PlayerInRoom> findByGameRoomAndUser(GameRoom gameRoom, User user);

    List<PlayerInRoom> findAllByGameRoom(GameRoom gameRoom);

    List<PlayerInRoom> findAllByUser(User user);

    boolean existsByGameRoomAndUser(GameRoom gameRoom, User user);

    long countByGameRoom(GameRoom gameRoom);

    void deleteByGameRoomAndUser(GameRoom gameRoom, User user);
}
