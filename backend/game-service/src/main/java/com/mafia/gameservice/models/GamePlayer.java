package com.mafia.gameservice.models;

import com.mafia.gameservice.enums.GameRole;
import jakarta.persistence.*;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(
        name = "game_players",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"game_id", "user_id"}),
                @UniqueConstraint(columnNames = {"game_id", "game_nick"})
        })
@Getter
@Setter
public class GamePlayer {
    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.TIME)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "game_nick", nullable = false, length = 50)
    private String gameNick;

    @Enumerated(EnumType.STRING)
    @Column(name = "assigned_role", nullable = false)
    private GameRole assignedRole;

    @Column(name = "is_alive", nullable = false)
    private boolean isAlive = true;

    @Version
    private Integer version;
}
