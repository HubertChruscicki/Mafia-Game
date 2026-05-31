package com.mafia.gameservice.repositories;

import com.mafia.gameservice.models.GameVote;
import com.mafia.gameservice.models.VotingSession;
import com.mafia.gameservice.enums.GamePhase;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface GameVoteRepository extends JpaRepository<GameVote, UUID> {
  long countByGame_IdAndPhaseAndDayNumber(UUID gameId, GamePhase phase, int dayNumber);

  Optional<GameVote> findByGame_IdAndPhaseAndDayNumberAndVoterId(
      UUID gameId, GamePhase phase, int dayNumber, UUID voterId);

  List<GameVote> findByGame_IdAndPhaseAndDayNumber(UUID gameId, GamePhase phase, int dayNumber);

  void deleteByGame_IdAndPhaseAndDayNumber(UUID gameId, GamePhase phase, int dayNumber);

  @Query("select count(v) from GameVote v where v.game.room.roomCode = :roomCode and v.phase = :phase")
  long countByRoomCodeAndPhase(@Param("roomCode") String roomCode, @Param("phase") GamePhase phase);

  @Query("select v from GameVote v where v.game.room.roomCode = :roomCode and v.phase = :phase")
  List<GameVote> findByRoomCodeAndPhase(@Param("roomCode") String roomCode, @Param("phase") GamePhase phase);

  @Modifying
  @Query("delete from GameVote v where v.game.room.roomCode = :roomCode and v.phase = :phase")
  void deleteByRoomCodeAndPhase(@Param("roomCode") String roomCode, @Param("phase") GamePhase phase);

  @Query("select count(v) from GameVote v where v.game.room.roomCode = :roomCode and v.phase = :phase and v.dayNumber = :dayNumber")
  long countByRoomCodeAndPhaseAndDayNumber(@Param("roomCode") String roomCode, @Param("phase") GamePhase phase, @Param("dayNumber") Integer dayNumber);

  @Query("select v from GameVote v where v.game.room.roomCode = :roomCode and v.phase = :phase and v.dayNumber = :dayNumber")
  List<GameVote> findByRoomCodeAndPhaseAndDayNumber(@Param("roomCode") String roomCode, @Param("phase") GamePhase phase, @Param("dayNumber") Integer dayNumber);

  @Query("select v from GameVote v where v.game.room.roomCode = :roomCode and v.phase = :phase and v.voterId = :voterId")
  Optional<GameVote> findByRoomCodeAndPhaseAndVoterId(@Param("roomCode") String roomCode, @Param("phase") GamePhase phase, @Param("voterId") UUID voterId);

  /**
   * Znajdź głos dla sesji i głosującego
   */
  @Query("SELECT v FROM GameVote v WHERE v.votingSession = :session AND v.voterId = :voterId")
  Optional<GameVote> findByVotingSessionAndVoterId(
      @Param("session") VotingSession session, @Param("voterId") UUID voterId);

  /**
   * Znajdź wszystkie głosy dla sesji
   */
  List<GameVote> findByVotingSession(VotingSession votingSession);

  /**
   * Znajdź ważne głosy dla sesji
   */
  @Query("SELECT v FROM GameVote v WHERE v.votingSession = :session AND v.isValid = :isValid")
  List<GameVote> findByVotingSessionAndIsValid(
      @Param("session") VotingSession session, @Param("isValid") boolean isValid);

  /**
   * Policz głosy dla sesji
   */
  long countByVotingSession(VotingSession votingSession);

  /**
   * Policz ważne głosy dla sesji
   */
  @Query("SELECT COUNT(v) FROM GameVote v WHERE v.votingSession = :session AND v.isValid = true")
  long countValidVotesByVotingSession(@Param("session") VotingSession session);
}
