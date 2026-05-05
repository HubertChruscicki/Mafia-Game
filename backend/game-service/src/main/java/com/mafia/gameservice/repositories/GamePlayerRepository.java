package com.mafia.gameservice.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.mafia.gameservice.models.Game;
import com.mafia.gameservice.models.GamePlayer;
import com.mafia.gameservice.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface GamePlayerRepository extends JpaRepository<GamePlayer, UUID> {
    List<GamePlayer> findAllByGameId(UUID gameId);

    /**
     * Znajdź gracza w grze po ID użytkownika
     */
    @Query("SELECT gp FROM GamePlayer gp WHERE gp.game = :game AND gp.user.id = :userId")
    Optional<GamePlayer> findByGameAndUser_Id(@Param("game") Game game, @Param("userId") UUID userId);

    /**
     * Znajdź gracza w grze po użytkowniku
     */
    Optional<GamePlayer> findByGameAndUser(Game game, User user);

    /**
     * Znajdź wszystkich graczy w grze
     */
    List<GamePlayer> findByGame(Game game);

    /**
     * Policz żywych graczy w grze
     */
    @Query("SELECT COUNT(gp) FROM GamePlayer gp WHERE gp.game = :game AND gp.isAlive = :isAlive")
    int countByGameAndIsAlive(@Param("game") Game game, @Param("isAlive") boolean isAlive);

    /**
     * Znajdź żywych graczy w grze
     */
    @Query("SELECT gp FROM GamePlayer gp WHERE gp.game = :game AND gp.isAlive = true")
    List<GamePlayer> findAlivePlayersByGame(@Param("game") Game game);

    /**
     * Policz graczy z daną rolą
     */
    @Query(
            "SELECT COUNT(gp) FROM GamePlayer gp WHERE gp.game = :game AND gp.assignedRole = :role AND gp.isAlive = :isAlive")
    int countByGameAndAssignedRoleAndIsAlive(
            @Param("game") Game game,
            @Param("role") com.mafia.gameservice.enums.GameRole role,
            @Param("isAlive") boolean isAlive);
}
