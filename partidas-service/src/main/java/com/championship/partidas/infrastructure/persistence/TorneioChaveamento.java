package com.championship.partidas.infrastructure.persistence;

import com.championship.partidas.domain.FormatoTorneio;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "torneio_chaveamento")
public class TorneioChaveamento {

    @Id
    @Column(name = "campeonato_id")
    private UUID campeonatoId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FormatoTorneio formato;

    @Column(name = "total_rounds")
    private Integer totalRounds;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "draw_order", nullable = false, columnDefinition = "jsonb")
    private String drawOrder;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "group_ids", columnDefinition = "jsonb")
    private String groupIds;

    @Column(name = "disputa_terceiro", nullable = false)
    private boolean disputaTerceiro = false;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected TorneioChaveamento() {
    }

    public TorneioChaveamento(UUID campeonatoId, FormatoTorneio formato, Integer totalRounds,
                               String drawOrder, String groupIds, boolean disputaTerceiro) {
        this.campeonatoId = campeonatoId;
        this.formato = formato;
        this.totalRounds = totalRounds;
        this.drawOrder = drawOrder;
        this.groupIds = groupIds;
        this.disputaTerceiro = disputaTerceiro;
        this.createdAt = Instant.now();
    }

    public UUID getCampeonatoId() {
        return campeonatoId;
    }

    public FormatoTorneio getFormato() {
        return formato;
    }

    public Integer getTotalRounds() {
        return totalRounds;
    }

    public String getDrawOrder() {
        return drawOrder;
    }

    public String getGroupIds() {
        return groupIds;
    }

    public boolean isDisputaTerceiro() {
        return disputaTerceiro;
    }
}
