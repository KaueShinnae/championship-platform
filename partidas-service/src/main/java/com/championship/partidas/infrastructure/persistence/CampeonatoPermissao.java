package com.championship.partidas.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "campeonato_permissao")
public class CampeonatoPermissao {

    @Embeddable
    public static class CampeonatoPermissaoId implements Serializable {

        @Column(name = "campeonato_id")
        private UUID campeonatoId;

        @Column(name = "usuario_id")
        private UUID usuarioId;

        protected CampeonatoPermissaoId() {
        }

        public CampeonatoPermissaoId(UUID campeonatoId, UUID usuarioId) {
            this.campeonatoId = campeonatoId;
            this.usuarioId = usuarioId;
        }

        public UUID getCampeonatoId() {
            return campeonatoId;
        }

        public UUID getUsuarioId() {
            return usuarioId;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (!(other instanceof CampeonatoPermissaoId that)) return false;
            return Objects.equals(campeonatoId, that.campeonatoId) && Objects.equals(usuarioId, that.usuarioId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(campeonatoId, usuarioId);
        }
    }

    @EmbeddedId
    private CampeonatoPermissaoId id;

    @Column(nullable = false, length = 10)
    private String papel;

    protected CampeonatoPermissao() {
    }

    public CampeonatoPermissao(UUID campeonatoId, UUID usuarioId, String papel) {
        this.id = new CampeonatoPermissaoId(campeonatoId, usuarioId);
        this.papel = papel;
    }

    public CampeonatoPermissaoId getId() {
        return id;
    }

    public String getPapel() {
        return papel;
    }
}
