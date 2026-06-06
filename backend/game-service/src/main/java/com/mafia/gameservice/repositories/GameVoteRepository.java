package com.mafia.gameservice.repositories;

import com.mafia.gameservice.models.GameVote;
import com.mafia.gameservice.models.VotingSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GameVoteRepository extends JpaRepository<GameVote, UUID> {

    Optional<GameVote> findByVotingSessionAndVoterId(VotingSession session, UUID voterId);

    List<GameVote> findByVotingSessionAndIsValid(VotingSession session, boolean isValid);

    long countByVotingSession(VotingSession session);

    @Query("SELECT v.targetUserId, COUNT(v) FROM GameVote v WHERE v.votingSession = :session AND v.isValid = true GROUP BY v.targetUserId")
    List<Object[]> countVotesByTarget(@Param("session") VotingSession session);

    /** All submitted votes (including decoy citizen votes at night). Used for UI display only. */
    @Query("SELECT v.targetUserId, COUNT(v) FROM GameVote v WHERE v.votingSession = :session GROUP BY v.targetUserId")
    List<Object[]> countAllVotesByTarget(@Param("session") VotingSession session);
}
