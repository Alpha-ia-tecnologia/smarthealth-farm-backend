package com.alphatech.cahosp.estoque.dominio;

import com.alphatech.cahosp.medicamento.dominio.Medicamento;
import com.alphatech.cahosp.unidade.dominio.Unidade;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

/**
 * Posicao de estoque de um medicamento em uma unidade (RF-EST-01/04/05). Guarda os parametros
 * de dimensionamento (nivel critico, estoque maximo, consumo medio, lead time) — derivados da
 * previsao nas fases seguintes (RF-PRV).
 *
 * <p>{@link #quantidade} e uma <strong>projecao do livro-razao</strong> (soma dos saldos dos
 * lotes), mantida transacionalmente pelo servico de movimentacao para leitura rapida. O
 * <em>status</em> (ok/atencao/critico) e calculado por servico de dominio, nao persistido.
 */
@Entity
@Table(name = "posicao_estoque",
        uniqueConstraints = @UniqueConstraint(name = "uk_posicao_med_unidade",
                columnNames = {"medicamento_id", "unidade_id"}))
@EntityListeners(AuditingEntityListener.class)
public class PosicaoEstoque {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "medicamento_id", nullable = false)
    private Medicamento medicamento;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "unidade_id", nullable = false)
    private Unidade unidade;

    @Column(nullable = false)
    private int quantidade;

    @Column(name = "nivel_critico", nullable = false)
    private int nivelCritico;

    @Column(name = "estoque_maximo", nullable = false)
    private int estoqueMaximo;

    @Column(name = "consumo_medio_diario", nullable = false)
    private int consumoMedioDiario;

    @Column(name = "tempo_medio_ressuprimento_dias", nullable = false)
    private int tempoMedioRessuprimentoDias;

    @CreatedDate
    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @LastModifiedDate
    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    protected PosicaoEstoque() {
        // JPA
    }

    public PosicaoEstoque(Medicamento medicamento, Unidade unidade, int quantidade,
                          int nivelCritico, int estoqueMaximo, int consumoMedioDiario,
                          int tempoMedioRessuprimentoDias) {
        this.medicamento = medicamento;
        this.unidade = unidade;
        this.quantidade = quantidade;
        this.nivelCritico = nivelCritico;
        this.estoqueMaximo = estoqueMaximo;
        this.consumoMedioDiario = consumoMedioDiario;
        this.tempoMedioRessuprimentoDias = tempoMedioRessuprimentoDias;
    }

    @PrePersist
    void aoCriar() {
        if (id == null) {
            id = UUID.randomUUID();
        }
    }

    /** Aplica a variacao de saldo de uma movimentacao (delta pode ser negativo). */
    public void aplicarDelta(int delta) {
        this.quantidade += delta;
    }

    public UUID getId() {
        return id;
    }

    public Medicamento getMedicamento() {
        return medicamento;
    }

    public Unidade getUnidade() {
        return unidade;
    }

    public int getQuantidade() {
        return quantidade;
    }

    public int getNivelCritico() {
        return nivelCritico;
    }

    public int getEstoqueMaximo() {
        return estoqueMaximo;
    }

    public int getConsumoMedioDiario() {
        return consumoMedioDiario;
    }

    public int getTempoMedioRessuprimentoDias() {
        return tempoMedioRessuprimentoDias;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public Instant getAtualizadoEm() {
        return atualizadoEm;
    }
}
