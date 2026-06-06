package com.mafia.gameservice.repositories;

import com.mafia.gameservice.enums.GamePhase;
import com.mafia.gameservice.enums.VotingStatus;
import com.mafia.gameservice.models.Game;
import com.mafia.gameservice.models.VotingSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VotingSessionRepository extends JpaRepository<VotingSession, UUID> {

    List<VotingSession> findByStatus(VotingStatus status);

    List<VotingSession> findByStatusAndEndsAtBefore(VotingStatus status, LocalDateTime dateTime);

    Optional<VotingSession> findByGameAndPhaseAndDayNumberAndStatus(
            Game game, GamePhase phase, int dayNumber, VotingStatus status);

    @Query("SELECT vs FROM VotingSession vs WHERE vs.game = :game AND vs.status = 'ACTIVE'")
    Optional<VotingSession> findActiveSessionByGame(@Param("game") Game game);
}
