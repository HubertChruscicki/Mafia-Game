package com.mafia.gameservice.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mafia.gameservice.dto.gameroom.CreateGameRoomReq;
import com.mafia.gameservice.dto.gameroom.GameRoomInfoReq;
import com.mafia.gameservice.dto.gameroom.JoinGameRoomReq;
import com.mafia.gameservice.dto.gameroom.LeaveGameRoomReq;
import com.mafia.gameservice.enums.GameRoomStatus;
import com.mafia.gameservice.models.GameRoom;
import com.mafia.gameservice.models.PlayerInRoom;
import com.mafia.gameservice.models.User;
import com.mafia.gameservice.repositories.GameRoomRepository;
import com.mafia.gameservice.repositories.PlayerInRoomRepository;
import com.mafia.gameservice.repositories.UserRepository;
import com.mafia.gameservice.support.GameTestFixtures;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class GameRoomServiceTest {

    @Mock private GameRoomRepository gameRoomRepository;
    @Mock private UserRepository userRepository;
    @Mock private PlayerInRoomRepository playerInRoomRepository;
    @Mock private SimpMessagingTemplate messagingTemplate;
    @Mock private PlayerInRoomService playerInRoomService;

    @InjectMocks
    private GameRoomService gameRoomService;

    private User host;

    @BeforeEach
    void setUp() {
        host = GameTestFixtures.user("host");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(host, "", List.of()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createRoomPersistsRoomAndAddsHost() {
        CreateGameRoomReq req = new CreateGameRoomReq();
        req.setName("My Room");
        req.setMaxPlayers(8);
        when(userRepository.findById(host.getId())).thenReturn(Optional.of(host));
        when(gameRoomRepository.existsByRoomCode(any())).thenReturn(false);
        when(gameRoomRepository.save(any(GameRoom.class))).thenAnswer(inv -> {
            GameRoom room = inv.getArgument(0);
            room.setId(UUID.randomUUID());
            return room;
        });

        var resp = gameRoomService.createRoom(req);

        assertThat(resp.getName()).isEqualTo("My Room");
        assertThat(resp.getRoomCode()).isNotBlank();
        verify(playerInRoomService).addPlayerToGameRoom(eq(host), any(GameRoom.class));
    }

    @Test
    void joinRoomRejectsClosedRoom() {
        GameRoom room = GameTestFixtures.openRoom(host);
        room.setGameRoomStatus(GameRoomStatus.GAME_IN_PROGRESS);
        when(userRepository.findById(host.getId())).thenReturn(Optional.of(host));
        when(gameRoomRepository.findByRoomCode("ABC123")).thenReturn(Optional.of(room));

        assertThatThrownBy(() -> gameRoomService.joinRoom(new JoinGameRoomReq("ABC123")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not OPEN");
    }

    @Test
    void joinRoomRejectsFullRoom() {
        GameRoom room = GameTestFixtures.openRoom(host);
        room.setMaxPlayers(2);
        User guest = GameTestFixtures.user("guest");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(guest, "", List.of()));

        when(userRepository.findById(guest.getId())).thenReturn(Optional.of(guest));
        when(gameRoomRepository.findByRoomCode("ABC123")).thenReturn(Optional.of(room));
        when(playerInRoomRepository.existsByGameRoomAndUser(room, guest)).thenReturn(false);
        when(playerInRoomRepository.countByGameRoom(room)).thenReturn(2L);

        assertThatThrownBy(() -> gameRoomService.joinRoom(new JoinGameRoomReq("ABC123")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("full");
    }

    @Test
    void joinRoomAddsNewPlayerAndBroadcasts() {
        GameRoom room = GameTestFixtures.openRoom(host);
        User guest = GameTestFixtures.user("guest");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(guest, "", List.of()));

        when(userRepository.findById(guest.getId())).thenReturn(Optional.of(guest));
        when(gameRoomRepository.findByRoomCode("ABC123")).thenReturn(Optional.of(room));
        when(playerInRoomRepository.existsByGameRoomAndUser(room, guest)).thenReturn(false);
        when(playerInRoomRepository.countByGameRoom(room)).thenReturn(1L);
        when(playerInRoomRepository.findAllByGameRoom(room)).thenReturn(List.of());

        var resp = gameRoomService.joinRoom(new JoinGameRoomReq("ABC123"));

        assertThat(resp.getRoomCode()).isEqualTo("ABC123");
        verify(playerInRoomService).addPlayerToGameRoom(guest, room);
        verify(messagingTemplate).convertAndSend(eq("/topic/game/ABC123/updated"), org.mockito.ArgumentMatchers.<Object>any());
    }

    @Test
    void leaveRoomAsHostDeletesRoom() {
        GameRoom room = GameTestFixtures.openRoom(host);
        PlayerInRoom membership = GameTestFixtures.playerInRoom(room, host);
        when(userRepository.findById(host.getId())).thenReturn(Optional.of(host));
        when(gameRoomRepository.findByRoomCode("ABC123")).thenReturn(Optional.of(room));
        when(playerInRoomRepository.findAllByGameRoom(room)).thenReturn(List.of(membership));

        var resp = gameRoomService.leaveRoom(new LeaveGameRoomReq("ABC123"));

        assertThat(resp.isSuccess()).isTrue();
        assertThat(resp.getMessage()).contains("deleted");
        verify(gameRoomRepository).delete(room);
        verify(messagingTemplate).convertAndSend(eq("/topic/game/ABC123/roomDeleted"), org.mockito.ArgumentMatchers.<Object>any());
    }

    @Test
    void leaveRoomAsGuestRemovesMembership() {
        GameRoom room = GameTestFixtures.openRoom(host);
        User guest = GameTestFixtures.user("guest");
        PlayerInRoom membership = GameTestFixtures.playerInRoom(room, guest);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(guest, "", List.of()));

        when(userRepository.findById(guest.getId())).thenReturn(Optional.of(guest));
        when(gameRoomRepository.findByRoomCode("ABC123")).thenReturn(Optional.of(room));
        when(playerInRoomRepository.findAllByGameRoom(room)).thenReturn(List.of(membership));
        when(playerInRoomRepository.findAllByGameRoom(room)).thenReturn(List.of(membership));

        var resp = gameRoomService.leaveRoom(new LeaveGameRoomReq("ABC123"));

        assertThat(resp.getMessage()).contains("Left room");
        verify(playerInRoomRepository).delete(membership);
        verify(gameRoomRepository, never()).delete(any());
    }

    @Test
    void getGameRoomInfoByCodeReturnsDetails() {
        GameRoom room = GameTestFixtures.openRoom(host);
        when(gameRoomRepository.findByRoomCode("ABC123")).thenReturn(Optional.of(room));
        when(playerInRoomRepository.findAllByGameRoom(room)).thenReturn(List.of(
                GameTestFixtures.playerInRoom(room, host)
        ));

        var info = gameRoomService.getGameRoomInfoByCode("ABC123");

        assertThat(info.getRoomCode()).isEqualTo("ABC123");
        assertThat(info.getHostUsername()).isEqualTo("host");
        assertThat(info.getPlayers()).hasSize(1);
    }

    @Test
    void searchByRoomCodeUsesExactLookup() {
        GameRoom room = GameTestFixtures.openRoom(host);
        when(gameRoomRepository.findByRoomCode("ABC123")).thenReturn(Optional.of(room));
        when(playerInRoomRepository.findAllByGameRoom(room)).thenReturn(List.of());

        var list = gameRoomService.getGameRoomsByFilter(new GameRoomInfoReq("ABC123", null));

        assertThat(list.getRooms()).hasSize(1);
    }

    @Test
    void searchByNameReturnsMatches() {
        GameRoom room = GameTestFixtures.openRoom(host);
        when(gameRoomRepository.findByNameContainingIgnoreCase("test")).thenReturn(List.of(room));
        when(playerInRoomRepository.findAllByGameRoom(room)).thenReturn(List.of());

        var rooms = gameRoomService.searchGameRoomsByName("test");

        assertThat(rooms).hasSize(1);
        assertThat(rooms.get(0).getName()).isEqualTo("Test Room");
    }

    @Test
    void getGameRoomInfoThrowsWhenMissing() {
        when(gameRoomRepository.findByRoomCode("MISSING")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> gameRoomService.getGameRoomInfoByCode("MISSING"))
                .isInstanceOf(NoSuchElementException.class);
    }
}
