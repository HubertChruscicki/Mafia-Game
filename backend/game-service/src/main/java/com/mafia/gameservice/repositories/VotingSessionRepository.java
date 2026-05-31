package com.mafia.gameservice.repositories;

import com.mafia.gameservice.models.Game;
import com.mafia.gameservice.models.VotingSession;
import com.mafia.gameservice.enums.GamePhase;
import com.mafia.gameservice.enums.VotingStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface VotingSessionRepository extends JpaRepository<VotingSession, UUID> {

  /**
   * Znajdź aktywną sesję głosowania dla danej gry, fazy i dnia
   */
  Optional<VotingSession> findByGameAndPhaseAndDayNumberAndStatus(
      Game game, GamePhase phase, int dayNumber, VotingStatus status);

  /**
   * Znajdź wszystkie sesje o danym statusie
   */
  List<VotingSession> findByStatus(VotingStatus status);

  /**
   * Znajdź sesje które wygasły (status ACTIVE i czas minął)
   */
  List<VotingSession> findByStatusAndEndsAtBefore(VotingStatus status, LocalDateTime dateTime);

  /**
   * Znajdź aktywną sesję dla gry
   */
  @Query(
      "SELECT vs FROM VotingSession vs WHERE vs.game = :game AND vs.status = 'ACTIVE' ORDER BY vs.createdAt DESC")
  Optional<VotingSession> findActiveSessionByGame(@Param("game") Game game);

  /**
   * Znajdź wszystkie sesje dla gry
   */
  List<VotingSession> findByGameOrderByCreatedAtDesc(Game game);

  /**
   * Sprawdź czy istnieje aktywna sesja dla gry
   */
  @Query("SELECT COUNT(vs) > 0 FROM VotingSession vs WHERE vs.game = :game AND vs.status = 'ACTIVE'")
  boolean existsActiveSessionForGame(@Param("game") Game game);
}
