package com.alphatech.cahosp.indicador.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Ponto do historico de um {@link IndicadorMeta} (RF-IND-05): valor medido em um periodo mensal,
 * alimentando a serie de tendencia exibida frente a meta.
 */
@Entity
@Table(name = "ponto_historico")
public class PontoHistorico {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "indicador_id", nullable = false)
    private IndicadorMeta indicador;

    @Column(nullable = false, length = 7)
    private String periodo;

    @Column(nullable = false)
    private int ordem;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal valor;

    protected PontoHistorico() {
        // JPA
    }

    public PontoHistorico(String periodo, int ordem, BigDecimal valor) {
        this.periodo = periodo;
        this.ordem = ordem;
        this.valor = valor;
    }

    @PrePersist
    void aoCriar() {
        if (id == null) {
            id = UUID.randomUUID();
        }
    }

    void vincular(IndicadorMeta indicador) {
        this.indicador = indicador;
    }

    public UUID getId() {
        return id;
    }

    public String getPeriodo() {
        return periodo;
    }

    public int getOrdem() {
        return ordem;
    }

    public BigDecimal getValor() {
        return valor;
    }
}
