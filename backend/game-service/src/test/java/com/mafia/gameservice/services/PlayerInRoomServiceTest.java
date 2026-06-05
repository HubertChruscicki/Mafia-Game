package com.mafia.gameservice.services;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mafia.gameservice.models.GameRoom;
import com.mafia.gameservice.models.User;
import com.mafia.gameservice.repositories.PlayerInRoomRepository;
import com.mafia.gameservice.support.GameTestFixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PlayerInRoomServiceTest {

    @Mock private PlayerInRoomRepository playerInRoomRepository;

    @InjectMocks
    private PlayerInRoomService playerInRoomService;

    @Test
    void addPlayerSkipsWhenAlreadyPresent() {
        User user = GameTestFixtures.user("alice");
        GameRoom room = GameTestFixtures.openRoom(user);
        when(playerInRoomRepository.existsByGameRoomAndUser(room, user)).thenReturn(true);

        playerInRoomService.addPlayerToGameRoom(user, room);

        verify(playerInRoomRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void addPlayerPersistsMembership() {
        User user = GameTestFixtures.user("alice");
        GameRoom room = GameTestFixtures.openRoom(user);
        when(playerInRoomRepository.existsByGameRoomAndUser(room, user)).thenReturn(false);

        playerInRoomService.addPlayerToGameRoom(user, room);

        verify(playerInRoomRepository).save(org.mockito.ArgumentMatchers.argThat(p ->
                p.getUser().equals(user) && p.getGameRoom().equals(room)));
    }
}
