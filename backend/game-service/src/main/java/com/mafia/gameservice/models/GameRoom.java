package com.mafia.gameservice.models;

import com.mafia.gameservice.enums.GameRoomStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "game_rooms")
@Getter
@Setter
public class GameRoom {
    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.TIME)
    private UUID id;

    @Column(unique = true, nullable = false, length = 6, updatable = false)
    private String roomCode;

    @Column(nullable = false, length = 100)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false, name = "host_id")
    private User host;

    @Column(nullable = false)
    private int maxPlayers;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GameRoomStatus gameRoomStatus;

    @Column(name = "mafia_count", nullable = false)
    private int mafiaCount = 1;

    @Column(name = "discussion_time_seconds", nullable = false)
    private int discussionTimeSeconds = 120;

    @Version
    private Integer version;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
