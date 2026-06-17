package com.alphatech.cahosp.estoque.dominio;

import com.alphatech.cahosp.insumo.dominio.Insumo;
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
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Lote de um insumo em uma unidade — unidade de rastreabilidade por validade/fabricante
 * (RF-EST-02/03). O saldo do lote ({@link #quantidade}) so muda via livro-razao
 * ({@link Movimentacao}); a entidade expoe metodos de dominio coerentes (sem setter cru).
 */
@Entity
@Table(name = "lote")
@EntityListeners(AuditingEntityListener.class)
public class Lote {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "insumo_id", nullable = false)
    private Insumo insumo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "unidade_id", nullable = false)
    private Unidade unidade;

    @Column(name = "numero_lote", nullable = false, length = 40)
    private String numeroLote;

    @Column(nullable = false)
    private LocalDate validade;

    @Column(nullable = false)
    private int quantidade;

    @Column(nullable = false, length = 80)
    private String fabricante;

    @CreatedDate
    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @LastModifiedDate
    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    protected Lote() {
        // JPA
    }

    public Lote(Insumo insumo, Unidade unidade, String numeroLote,
                LocalDate validade, int quantidade, String fabricante) {
        this.insumo = insumo;
        this.unidade = unidade;
        this.numeroLote = numeroLote;
        this.validade = validade;
        this.quantidade = quantidade;
        this.fabricante = fabricante;
    }

    @PrePersist
    void aoCriar() {
        if (id == null) {
            id = UUID.randomUUID();
        }
    }

    /** Soma ao saldo do lote (entrada). */
    public void adicionar(int quantidade) {
        this.quantidade += quantidade;
    }

    /** Subtrai do saldo do lote (saida/transferencia). O chamador garante saldo suficiente. */
    public void subtrair(int quantidade) {
        this.quantidade -= quantidade;
    }

    /** Define o saldo absoluto (ajuste/recontagem de inventario). */
    public void ajustarPara(int quantidade) {
        this.quantidade = quantidade;
    }

    public UUID getId() {
        return id;
    }

    public Insumo getInsumo() {
        return insumo;
    }

    public Unidade getUnidade() {
        return unidade;
    }

    public String getNumeroLote() {
        return numeroLote;
    }

    public LocalDate getValidade() {
        return validade;
    }

    public int getQuantidade() {
        return quantidade;
    }

    public String getFabricante() {
        return fabricante;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public Instant getAtualizadoEm() {
        return atualizadoEm;
    }
}
