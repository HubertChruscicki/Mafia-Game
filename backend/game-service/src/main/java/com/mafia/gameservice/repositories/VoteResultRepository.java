package com.mafia.gameservice.repositories;

import com.mafia.gameservice.models.VoteResult;
import com.mafia.gameservice.models.VotingSession;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface VoteResultRepository extends JpaRepository<VoteResult, UUID> {

  /**
   * Znajdź wszystkie wyniki dla sesji głosowania
   */
  List<VoteResult> findByVotingSession(VotingSession votingSession);

  /**
   * Znajdź wynik dla konkretnego gracza w sesji
   */
  @Query(
      "SELECT vr FROM VoteResult vr WHERE vr.votingSession = :session AND vr.targetUser.id = :targetUserId")
  Optional<VoteResult> findByVotingSessionAndTargetUser_Id(
      @Param("session") VotingSession session, @Param("targetUserId") UUID targetUserId);

  /**
   * Znajdź wyniki posortowane według liczby głosów (malejąco)
   */
  @Query(
      "SELECT vr FROM VoteResult vr WHERE vr.votingSession = :session ORDER BY vr.voteCount DESC")
  List<VoteResult> findByVotingSessionOrderByVoteCountDesc(
      @Param("session") VotingSession session);

  /**
   * Znajdź wyniki posortowane według głosów mafii (dla nocnego głosowania)
   */
  @Query(
      "SELECT vr FROM VoteResult vr WHERE vr.votingSession = :session ORDER BY vr.mafiaVoteCount DESC")
  List<VoteResult> findByVotingSessionOrderByMafiaVoteCountDesc(
      @Param("session") VotingSession session);

  /**
   * Usuń wszystkie wyniki dla sesji
   */
  void deleteByVotingSession(VotingSession votingSession);
}
