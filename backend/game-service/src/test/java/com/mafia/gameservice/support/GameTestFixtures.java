package com.mafia.gameservice.support;

import com.mafia.gameservice.enums.GamePhase;
import com.mafia.gameservice.enums.GameRole;
import com.mafia.gameservice.enums.GameRoomStatus;
import com.mafia.gameservice.enums.GameStatus;
import com.mafia.gameservice.enums.VotingStatus;
import com.mafia.gameservice.models.Game;
import com.mafia.gameservice.models.GamePlayer;
import com.mafia.gameservice.models.GameRoom;
import com.mafia.gameservice.models.PlayerInRoom;
import com.mafia.gameservice.models.User;
import com.mafia.gameservice.models.VotingSession;
import java.time.LocalDateTime;
import java.util.UUID;

public final class GameTestFixtures {

    private GameTestFixtures() {
    }

    public static User user(String username) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername(username);
        user.setEmail(username + "@example.com");
        user.setAdmin(false);
        return user;
    }

    public static GameRoom openRoom(User host) {
        GameRoom room = new GameRoom();
        room.setId(UUID.randomUUID());
        room.setRoomCode("ABC123");
        room.setName("Test Room");
        room.setHost(host);
        room.setMaxPlayers(10);
        room.setMafiaCount(1);
        room.setDiscussionTimeSeconds(60);
        room.setGameRoomStatus(GameRoomStatus.OPEN);
        room.setCreatedAt(LocalDateTime.now());
        return room;
    }

    public static Game activeGame(GameRoom room) {
        Game game = new Game();
        game.setId(UUID.randomUUID());
        game.setRoom(room);
        game.setStatus(GameStatus.IN_PROGRESS);
        game.setCurrentPhase(GamePhase.NIGHT_VOTE);
        game.setCurrentDayNumber(1);
        game.setStartedAt(LocalDateTime.now());
        return game;
    }

    public static GamePlayer alivePlayer(Game game, User user, GameRole role) {
        GamePlayer player = new GamePlayer();
        player.setId(UUID.randomUUID());
        player.setGame(game);
        player.setUser(user);
        player.setGameNick(user.getUsername());
        player.setAssignedRole(role);
        player.setAlive(true);
        return player;
    }

    public static PlayerInRoom playerInRoom(GameRoom room, User user) {
        PlayerInRoom pir = new PlayerInRoom();
        pir.setId(UUID.randomUUID());
        pir.setGameRoom(room);
        pir.setUser(user);
        pir.setJoinedAt(LocalDateTime.now());
        return pir;
    }

    public static VotingSession activeSession(Game game, GamePhase phase) {
        VotingSession session = new VotingSession();
        session.setId(UUID.randomUUID());
        session.setGame(game);
        session.setPhase(phase);
        session.setDayNumber(game.getCurrentDayNumber());
        session.setStartedAt(LocalDateTime.now());
        session.setEndsAt(LocalDateTime.now().plusSeconds(60));
        session.setStatus(VotingStatus.ACTIVE);
        session.setTotalEligibleVoters(3);
        session.setVotesReceived(0);
        return session;
    }
}
