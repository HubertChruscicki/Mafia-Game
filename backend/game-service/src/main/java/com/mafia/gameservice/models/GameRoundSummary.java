package com.mafia.gameservice.models;

import com.mafia.gameservice.enums.EliminationCause;
import com.mafia.gameservice.enums.GamePhase;
import jakarta.persistence.*;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "game_round_summaries", indexes = {
        @Index(name = "idx_game_day_phase", columnList = "game_id,day_number,phase")
})
@Getter
@Setter
public class GameRoundSummary {
    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;

    @Column(name = "day_number", nullable = false)
    private int dayNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "phase", nullable = false)
    private GamePhase phase;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_eliminated_id")
    private GamePlayer playerEliminated;

    @Enumerated(EnumType.STRING)
    @Column(name = "elimination_cause")
    private EliminationCause cause;

    @Column(name = "summary_text", length = 1000)
    private String summaryText;
}
