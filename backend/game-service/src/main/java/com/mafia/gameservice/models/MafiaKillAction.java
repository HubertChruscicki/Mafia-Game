package com.mafia.gameservice.models;

import jakarta.persistence.*;

@Entity
@DiscriminatorValue("MAFIA_KILL")
public class MafiaKillAction extends GameAction {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_id", nullable = false)
    private GamePlayer target;

    public GamePlayer getTarget() {
        return target;
    }

    public void setTarget(GamePlayer target) {
        this.target = target;
    }
}

