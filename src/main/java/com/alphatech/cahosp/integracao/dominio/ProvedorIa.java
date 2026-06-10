package com.alphatech.cahosp.integracao.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Provedor de IA generativa registrado no AI Gateway (RF-INT-06 / RF-SEG-04): papel
 * (primario/fallback/standby), custo por 1k tokens, volume mensal e flag de anonimizacao.
 * {@link #codigo} e o identificador de negocio (ex.: {@code ia-deepseek}), usado na idempotencia.
 */
@Entity
@Table(name = "provedor_ia")
@EntityListeners(AuditingEntityListener.class)
public class ProvedorIa {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(nullable = false, length = 40, unique = true)
    private String codigo;

    @Column(nullable = false, length = 80)
    private String nome;

    @Column(nullable = false)
    private boolean ativo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PapelIa papel;

    @Column(name = "custo_por_1k_tokens", nullable = false, precision = 10, scale = 4)
    private BigDecimal custoPor1kTokens;

    @Column(name = "chamadas_mes", nullable = false)
    private long chamadasMes;

    @Column(nullable = false)
    private boolean anonimizacao;

    @Column(nullable = false)
    private int ordem;

    @CreatedDate
    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @LastModifiedDate
    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    protected ProvedorIa() {
    }

    public ProvedorIa(String codigo, String nome, boolean ativo, PapelIa papel,
                      BigDecimal custoPor1kTokens, long chamadasMes, boolean anonimizacao, int ordem) {
        this.codigo = codigo;
        this.nome = nome;
        this.ativo = ativo;
        this.papel = papel;
        this.custoPor1kTokens = custoPor1kTokens;
        this.chamadasMes = chamadasMes;
        this.anonimizacao = anonimizacao;
        this.ordem = ordem;
    }

    @PrePersist
    void gerarId() {
        if (id == null) {
            id = UUID.randomUUID();
        }
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

    public boolean isAtivo() {
        return ativo;
    }

    public PapelIa getPapel() {
        return papel;
    }

    public BigDecimal getCustoPor1kTokens() {
        return custoPor1kTokens;
    }

    public long getChamadasMes() {
        return chamadasMes;
    }

    public boolean isAnonimizacao() {
        return anonimizacao;
    }

    public int getOrdem() {
        return ordem;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public Instant getAtualizadoEm() {
        return atualizadoEm;
    }
}
