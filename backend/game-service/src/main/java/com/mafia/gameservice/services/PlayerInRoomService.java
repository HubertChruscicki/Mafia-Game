package com.mafia.gameservice.services;

import com.mafia.gameservice.models.GameRoom;
import com.mafia.gameservice.models.PlayerInRoom;
import com.mafia.gameservice.models.User;
import com.mafia.gameservice.repositories.PlayerInRoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PlayerInRoomService {

    private final PlayerInRoomRepository playerInRoomRepository;

    public void addPlayerToGameRoom(User user, GameRoom room) {
        if (playerInRoomRepository.existsByGameRoomAndUser(room, user)) {
            return;
        }
        PlayerInRoom playerInRoom = new PlayerInRoom();
        playerInRoom.setUser(user);
        playerInRoom.setGameRoom(room);
        playerInRoomRepository.save(playerInRoom);
    }
}
