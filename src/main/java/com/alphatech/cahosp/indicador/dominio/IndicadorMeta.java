package com.alphatech.cahosp.indicador.dominio;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Indicador de desempenho do projeto frente a meta do edital (RF-IND). Modelagem fiel ao tipo
 * {@code IndicadorMeta} do front: linha de base ({@link #baseline}), valor atual ({@link #atual}),
 * meta ({@link #meta}) e o {@link #historico} mensal da medicao.
 *
 * <p>{@link #codigo} e o codigo de negocio legivel (ex.: {@code ind-ruptura}), estavel e usado pelo
 * {@code IndicadorSeeder} para idempotencia. {@link #melhorMenor} indica a direcao desejada (true =
 * quanto menor, melhor). As derivacoes (progresso ate a meta, meta atingida, variacao) sao
 * calculadas por servico de dominio, nao persistidas.
 */
@Entity
@Table(name = "indicador")
@EntityListeners(AuditingEntityListener.class)
public class IndicadorMeta {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(nullable = false, length = 40, unique = true)
    private String codigo;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(name = "unidade_medida", nullable = false, length = 20)
    private String unidadeMedida;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal baseline;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal atual;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal meta;

    @Column(name = "meta_reducao_pct", nullable = false)
    private int metaReducaoPct;

    @Column(name = "melhor_menor", nullable = false)
    private boolean melhorMenor;

    @Column(nullable = false)
    private int ordem;

    @OneToMany(mappedBy = "indicador", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("ordem ASC")
    private List<PontoHistorico> historico = new ArrayList<>();

    @CreatedDate
    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @LastModifiedDate
    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    protected IndicadorMeta() {
        // JPA
    }

    public IndicadorMeta(String codigo, String nome, String unidadeMedida, BigDecimal baseline,
                         BigDecimal atual, BigDecimal meta, int metaReducaoPct, boolean melhorMenor,
                         int ordem) {
        this.codigo = codigo;
        this.nome = nome;
        this.unidadeMedida = unidadeMedida;
        this.baseline = baseline;
        this.atual = atual;
        this.meta = meta;
        this.metaReducaoPct = metaReducaoPct;
        this.melhorMenor = melhorMenor;
        this.ordem = ordem;
    }

    @PrePersist
    void aoCriar() {
        if (id == null) {
            id = UUID.randomUUID();
        }
    }

    /** Adiciona um ponto ao historico, mantendo a referencia inversa. */
    public void adicionarPonto(PontoHistorico ponto) {
        ponto.vincular(this);
        this.historico.add(ponto);
    }

    public UUID getId() {
        return id;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getNome() {
        return nome;
    }

    public String getUnidadeMedida() {
        return unidadeMedida;
    }

    public BigDecimal getBaseline() {
        return baseline;
    }

    public BigDecimal getAtual() {
        return atual;
    }

    public BigDecimal getMeta() {
        return meta;
    }

    public int getMetaReducaoPct() {
        return metaReducaoPct;
    }

    public boolean isMelhorMenor() {
        return melhorMenor;
    }

    public int getOrdem() {
        return ordem;
    }

    public List<PontoHistorico> getHistorico() {
        return historico;
    }
}
