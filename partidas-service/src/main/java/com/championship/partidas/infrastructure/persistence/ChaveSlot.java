package com.championship.partidas.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/**
 * Slot ocupado no bracket: o time que entra na rodada {@code round} na posição
 * {@code slot}. Quando os dois slots de um confronto (2p e 2p+1) estão
 * ocupados, a partida da posição p é criada. Também representa byes.
 */
@Entity
@Table(name = "chave_slot")
public class ChaveSlot {

    @Embeddable
    public static class ChaveSlotId implements Serializable {

        @Column(name = "campeonato_id")
        private UUID campeonatoId;

        @Column(name = "round")
        private int round;

        @Column(name = "slot")
        private int slot;

        protected ChaveSlotId() {
        }

        public ChaveSlotId(UUID campeonatoId, int round, int slot) {
            this.campeonatoId = campeonatoId;
            this.round = round;
            this.slot = slot;
        }

        public int getRound() {
            return round;
        }

        public int getSlot() {
            return slot;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (!(other instanceof ChaveSlotId that)) return false;
            return round == that.round && slot == that.slot && Objects.equals(campeonatoId, that.campeonatoId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(campeonatoId, round, slot);
        }
    }

    @EmbeddedId
    private ChaveSlotId id;

    @Column(name = "team_id", nullable = false)
    private UUID teamId;

    @Column(name = "team_name", nullable = false, length = 100)
    private String teamName;

    protected ChaveSlot() {
    }

    public ChaveSlot(UUID campeonatoId, int round, int slot, UUID teamId, String teamName) {
        this.id = new ChaveSlotId(campeonatoId, round, slot);
        this.teamId = teamId;
        this.teamName = teamName;
    }

    public ChaveSlotId getId() {
        return id;
    }

    public UUID getTeamId() {
        return teamId;
    }

    public String getTeamName() {
        return teamName;
    }
}
