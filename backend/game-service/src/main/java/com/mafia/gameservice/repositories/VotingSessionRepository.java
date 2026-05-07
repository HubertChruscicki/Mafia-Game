package com.mafia.gameservice.repositories;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.mafia.gameservice.enums.VotingStatus;
import com.mafia.gameservice.models.VotingSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface VotingSessionRepository extends JpaRepository<VotingSession, UUID> {
    /**
     * Znajdź wszystkie sesje o danym statusie
     */
    List<VotingSession> findByStatus(VotingStatus status);

    /**
     * Znajdź sesje które wygasły (status ACTIVE i czas minął)
     */
    List<VotingSession> findByStatusAndEndsAtBefore(VotingStatus status, LocalDateTime dateTime);

}
