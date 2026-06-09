package com.alphatech.cahosp.estoque.dominio;

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
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

/**
 * Lancamento no livro-razao do estoque (RF-EST-06). <strong>Imutavel</strong> apos criada —
 * nao ha setters, atualizacao nem exclusao: a rastreabilidade sanitaria exige um historico
 * append-only. Correcoes sao novos lancamentos (ex.: {@code AJUSTE}).
 */
@Entity
@Table(name = "movimentacao")
@EntityListeners(AuditingEntityListener.class)
public class Movimentacao {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lote_id", nullable = false)
    private Lote lote;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "medicamento_id", nullable = false)
    private Medicamento medicamento;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "unidade_id", nullable = false)
    private Unidade unidade;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoMovimentacao tipo;

    @Column(nullable = false)
    private int quantidade;

    @Column(name = "data_hora", nullable = false)
    private Instant dataHora;

    @Column(nullable = false, length = 120)
    private String responsavel;

    @Column(nullable = false, length = 60)
    private String documento;

    @CreatedDate
    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    protected Movimentacao() {
        // JPA
    }

    public Movimentacao(Lote lote, TipoMovimentacao tipo, int quantidade,
                        Instant dataHora, String responsavel, String documento) {
        this.lote = lote;
        this.medicamento = lote.getMedicamento();
        this.unidade = lote.getUnidade();
        this.tipo = tipo;
        this.quantidade = quantidade;
        this.dataHora = dataHora;
        this.responsavel = responsavel;
        this.documento = documento;
    }

    @PrePersist
    void aoCriar() {
        if (id == null) {
            id = UUID.randomUUID();
        }
    }

    public UUID getId() {
        return id;
    }

    public Lote getLote() {
        return lote;
    }

    public Medicamento getMedicamento() {
        return medicamento;
    }

    public Unidade getUnidade() {
        return unidade;
    }

    public TipoMovimentacao getTipo() {
        return tipo;
    }

    public int getQuantidade() {
        return quantidade;
    }

    public Instant getDataHora() {
        return dataHora;
    }

    public String getResponsavel() {
        return responsavel;
    }

    public String getDocumento() {
        return documento;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }
}
