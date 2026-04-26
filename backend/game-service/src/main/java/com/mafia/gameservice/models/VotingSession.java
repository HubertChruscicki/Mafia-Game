package com.mafia.gameservice.models;
import com.mafia.gameservice.enums.GamePhase;
import com.mafia.gameservice.enums.VotingStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

/**
 * Sesja głosowania - reprezentuje pojedyncze głosowanie w grze
 * Każda faza (DAY_VOTE, NIGHT_VOTE) ma swoją sesję
 */
@Entity
@Table(
        name = "voting_sessions",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_voting_session_game_phase_day",
                        columnNames = {"game_id", "phase", "day_number"})
        },
        indexes = {
                @Index(name = "idx_voting_session_game_status", columnList = "game_id,status"),
                @Index(name = "idx_voting_session_status_ends_at", columnList = "status,ends_at")
        })
@Getter
@Setter
public class VotingSession {
    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.TIME)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GamePhase phase;

    @Column(name = "day_number", nullable = false)
    private int dayNumber;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "ends_at", nullable = false)
    private LocalDateTime endsAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private VotingStatus status;

    @Column(name = "total_eligible_voters", nullable = false)
    private int totalEligibleVoters;

    @Column(name = "votes_received", nullable = false)
    private int votesReceived = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "result_user_id")
    private User resultUser;

    @Column(name = "is_tie", nullable = false)
    private boolean isTie = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Version
    private Integer version;

    @PrePersist
    private void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.status == null) {
            this.status = VotingStatus.ACTIVE;
        }
    }
}
