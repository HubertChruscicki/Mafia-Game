package com.mafia.gameservice.models;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

/**
 * Agregowane wyniki głosowania dla każdego gracza
 * Używane do wizualizacji w czasie rzeczywistym
 */
@Entity
@Table(
        name = "vote_results",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_vote_result_session_target",
                        columnNames = {"voting_session_id", "target_user_id"})
        },
        indexes = {@Index(name = "idx_vote_results_session", columnList = "voting_session_id")})
@Getter
@Setter
public class VoteResult {
    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.TIME)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voting_session_id", nullable = false)
    private VotingSession votingSession;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_user_id", nullable = false)
    private User targetUser;

    @Column(name = "vote_count", nullable = false)
    private int voteCount = 0;

    @Column(name = "mafia_vote_count", nullable = false)
    private int mafiaVoteCount = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    private void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.updatedAt == null) {
            this.updatedAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    private void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}