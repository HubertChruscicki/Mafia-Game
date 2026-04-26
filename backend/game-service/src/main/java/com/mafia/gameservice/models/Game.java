package com.mafia.gameservice.models;

import com.mafia.gameservice.enums.GamePhase;
import com.mafia.gameservice.enums.GameStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(
        name = "games",
        indexes = {
                @Index(name = "idx_game_room_status", columnList = "room_id,status"),
                @Index(name = "idx_game_room_created", columnList = "room_id,created_at")
        })
@Getter
@Setter
public class Game {
    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.TIME)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private GameRoom room;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GameStatus status;

    @Version
    private Integer version;

    @Column(name = "current_day_number", nullable = false)
    private int currentDayNumber = 1;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_phase", nullable = false)
    private GamePhase currentPhase;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @OneToMany(mappedBy = "game", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<GamePlayer> players = new HashSet<>();

    @PrePersist
    private void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.currentPhase == null) {
            this.currentPhase = GamePhase.NIGHT_VOTE; // initial phase
        }
    }
}
