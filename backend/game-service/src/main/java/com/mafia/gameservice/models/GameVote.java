package com.mafia.gameservice.models;


import com.mafia.gameservice.enums.GamePhase;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(
        name = "game_votes",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_vote_session_voter",
                        columnNames = {"voting_session_id", "voter_id"})
        },
        indexes = {
                @Index(name = "idx_game_phase_day", columnList = "game_id,phase,day_number"),
                @Index(name = "idx_game_phase_day_target", columnList = "game_id,phase,day_number,target_user_id"),
                @Index(name = "idx_vote_session", columnList = "voting_session_id"),
                @Index(name = "idx_vote_session_valid", columnList = "voting_session_id,is_valid")
        })
@Getter
@Setter
public class GameVote {
    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.TIME)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voting_session_id")
    private VotingSession votingSession;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GamePhase phase;

    @Column(name = "day_number", nullable = false)
    private int dayNumber;

    @Column(name = "voter_id", nullable = false)
    private UUID voterId;

    @Column(name = "target_user_id", nullable = false)
    private UUID targetUserId;

    @Column(name = "is_valid", nullable = false)
    private boolean isValid = true;

    @CreationTimestamp
    @Column(name = "voted_at", nullable = false, updatable = false)
    private LocalDateTime votedAt;
}
