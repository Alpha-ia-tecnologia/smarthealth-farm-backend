package com.alphatech.cahosp.previsao.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.util.UUID;

/**
 * Ponto da serie temporal de uma {@link Previsao} (RF-PRV-02): um periodo mensal com o valor
 * realizado e/ou previsto, e bandas de confianca opcionais. Nos meses de previsao futura,
 * {@code realizado} e nulo.
 */
@Entity
@Table(name = "ponto_serie")
public class PontoSerie {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "previsao_id", nullable = false)
    private Previsao previsao;

    @Column(nullable = false, length = 7)
    private String periodo;

    @Column(nullable = false)
    private int ordem;

    private Integer realizado;

    private Integer previsto;

    @Column(name = "limite_inferior")
    private Integer limiteInferior;

    @Column(name = "limite_superior")
    private Integer limiteSuperior;

    protected PontoSerie() {
        // JPA
    }

    public PontoSerie(String periodo, int ordem, Integer realizado, Integer previsto,
                      Integer limiteInferior, Integer limiteSuperior) {
        this.periodo = periodo;
        this.ordem = ordem;
        this.realizado = realizado;
        this.previsto = previsto;
        this.limiteInferior = limiteInferior;
        this.limiteSuperior = limiteSuperior;
    }

    @PrePersist
    void aoCriar() {
        if (id == null) {
            id = UUID.randomUUID();
        }
    }

    void vincular(Previsao previsao) {
        this.previsao = previsao;
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

    public Integer getRealizado() {
        return realizado;
    }

    public Integer getPrevisto() {
        return previsto;
    }

    public Integer getLimiteInferior() {
        return limiteInferior;
    }

    public Integer getLimiteSuperior() {
        return limiteSuperior;
    }
}
