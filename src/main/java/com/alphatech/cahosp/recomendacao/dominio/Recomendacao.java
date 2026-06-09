package com.alphatech.cahosp.recomendacao.dominio;

import com.alphatech.cahosp.comum.excecao.RegraNegocioException;
import com.alphatech.cahosp.medicamento.dominio.Medicamento;
import com.alphatech.cahosp.unidade.dominio.Unidade;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Recomendacao de reposicao ou redistribuicao gerada por regra (RF-REC). Dimensionada pela
 * previsao/estoque pelo motor {@code GeradorRecomendacao}:
 * <ul>
 *   <li>{@link TipoRecomendacao#REPOSICAO} — restaura o nivel de seguranca de {@link #unidadeDestino}
 *       (sem {@link #unidadeOrigem});</li>
 *   <li>{@link TipoRecomendacao#REDISTRIBUICAO} — transfere de {@link #unidadeOrigem} (excedente)
 *       para {@link #unidadeDestino} (em risco).</li>
 * </ul>
 *
 * <p>Dado <strong>derivado</strong> (gerado, nao lancado a mao). {@link #economiaEstimada} e R$
 * ({@link BigDecimal}); {@link #justificativa} explica a recomendacao (RF-REC-04).
 */
@Entity
@Table(name = "recomendacao")
@EntityListeners(AuditingEntityListener.class)
public class Recomendacao {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoRecomendacao tipo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "medicamento_id", nullable = false)
    private Medicamento medicamento;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "unidade_destino_id", nullable = false)
    private Unidade unidadeDestino;

    /** So presente em redistribuicao (unidade de origem do excedente). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unidade_origem_id")
    private Unidade unidadeOrigem;

    @Column(nullable = false)
    private int quantidade;

    @Column(nullable = false, length = 400)
    private String justificativa;

    @Enumerated(EnumType.STRING)
    @Column(name = "origem_motor", nullable = false, length = 30)
    private OrigemMotor origemMotor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Prioridade prioridade;

    @Column(name = "economia_estimada", nullable = false, precision = 12, scale = 2)
    private BigDecimal economiaEstimada;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusRecomendacao status;

    @CreatedDate
    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @LastModifiedDate
    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    protected Recomendacao() {
        // JPA
    }

    public Recomendacao(TipoRecomendacao tipo, Medicamento medicamento, Unidade unidadeDestino,
                        Unidade unidadeOrigem, int quantidade, String justificativa,
                        OrigemMotor origemMotor, Prioridade prioridade, BigDecimal economiaEstimada) {
        this.tipo = tipo;
        this.medicamento = medicamento;
        this.unidadeDestino = unidadeDestino;
        this.unidadeOrigem = unidadeOrigem;
        this.quantidade = quantidade;
        this.justificativa = justificativa;
        this.origemMotor = origemMotor;
        this.prioridade = prioridade;
        this.economiaEstimada = economiaEstimada;
        this.status = StatusRecomendacao.PENDENTE;
    }

    @PrePersist
    void aoCriar() {
        if (id == null) {
            id = UUID.randomUUID();
        }
    }

    /** Aprova a recomendacao (RF-REC — acao de Gestor). Apenas a partir de {@code PENDENTE}. */
    public void aprovar() {
        if (status != StatusRecomendacao.PENDENTE) {
            throw new RegraNegocioException(
                    "Apenas recomendacoes pendentes podem ser aprovadas (status atual: "
                            + status.rotulo() + ").");
        }
        this.status = StatusRecomendacao.APROVADA;
    }

    /** Marca como executada (RF-REC-05). Apenas a partir de {@code APROVADA}. */
    public void executar() {
        if (status != StatusRecomendacao.APROVADA) {
            throw new RegraNegocioException(
                    "Apenas recomendacoes aprovadas podem ser executadas (status atual: "
                            + status.rotulo() + ").");
        }
        this.status = StatusRecomendacao.EXECUTADA;
    }

    public UUID getId() {
        return id;
    }

    public TipoRecomendacao getTipo() {
        return tipo;
    }

    public Medicamento getMedicamento() {
        return medicamento;
    }

    public Unidade getUnidadeDestino() {
        return unidadeDestino;
    }

    public Unidade getUnidadeOrigem() {
        return unidadeOrigem;
    }

    public int getQuantidade() {
        return quantidade;
    }

    public String getJustificativa() {
        return justificativa;
    }

    public OrigemMotor getOrigemMotor() {
        return origemMotor;
    }

    public Prioridade getPrioridade() {
        return prioridade;
    }

    public BigDecimal getEconomiaEstimada() {
        return economiaEstimada;
    }

    public StatusRecomendacao getStatus() {
        return status;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public Instant getAtualizadoEm() {
        return atualizadoEm;
    }
}
