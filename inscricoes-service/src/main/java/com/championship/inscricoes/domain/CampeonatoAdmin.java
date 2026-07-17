package com.championship.inscricoes.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/** Administrador delegado de um campeonato (além do dono). */
@Entity
@Table(name = "campeonato_admin")
public class CampeonatoAdmin {

    @Embeddable
    public static class CampeonatoAdminId implements Serializable {

        @Column(name = "campeonato_id")
        private UUID campeonatoId;

        @Column(name = "usuario_id")
        private UUID usuarioId;

        protected CampeonatoAdminId() {
        }

        public CampeonatoAdminId(UUID campeonatoId, UUID usuarioId) {
            this.campeonatoId = campeonatoId;
            this.usuarioId = usuarioId;
        }

        public UUID getUsuarioId() {
            return usuarioId;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (!(other instanceof CampeonatoAdminId that)) return false;
            return Objects.equals(campeonatoId, that.campeonatoId) && Objects.equals(usuarioId, that.usuarioId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(campeonatoId, usuarioId);
        }
    }

    @EmbeddedId
    private CampeonatoAdminId id;

    protected CampeonatoAdmin() {
    }

    public CampeonatoAdmin(UUID campeonatoId, UUID usuarioId) {
        this.id = new CampeonatoAdminId(campeonatoId, usuarioId);
    }

    public CampeonatoAdminId getId() {
        return id;
    }
}
