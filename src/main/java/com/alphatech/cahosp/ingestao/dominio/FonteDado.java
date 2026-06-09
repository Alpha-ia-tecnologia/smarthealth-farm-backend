package com.alphatech.cahosp.ingestao.dominio;

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

import java.time.Instant;
import java.util.UUID;

/**
 * Fonte de dados ingerida na plataforma (RF-DAD-01/02/07): sistema de origem, status de
 * sincronizacao, volume, qualidade e procedencia. {@link #codigo} e o identificador de negocio
 * legivel (ex.: {@code f-sih}), usado pelo seeder para idempotencia.
 */
@Entity
@Table(name = "fonte_dado")
@EntityListeners(AuditingEntityListener.class)
public class FonteDado {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(nullable = false, length = 40, unique = true)
    private String codigo;

    @Column(nullable = false, length = 150)
    private String nome;

    @Column(nullable = false, length = 80)
    private String geracao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusFonte status;

    @Column(name = "ultima_ingestao", nullable = false)
    private Instant ultimaIngestao;

    @Column(nullable = false)
    private long registros;

    @Column(nullable = false)
    private int qualidade;

    @Column(nullable = false, length = 200)
    private String procedencia;

    @Column(nullable = false)
    private int ordem;

    @CreatedDate
    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @LastModifiedDate
    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    protected FonteDado() {
    }

    public FonteDado(String codigo, String nome, String geracao, StatusFonte status,
                     Instant ultimaIngestao, long registros, int qualidade, String procedencia,
                     int ordem) {
        this.codigo = codigo;
        this.nome = nome;
        this.geracao = geracao;
        this.status = status;
        this.ultimaIngestao = ultimaIngestao;
        this.registros = registros;
        this.qualidade = qualidade;
        this.procedencia = procedencia;
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

    public String getGeracao() {
        return geracao;
    }

    public StatusFonte getStatus() {
        return status;
    }

    public Instant getUltimaIngestao() {
        return ultimaIngestao;
    }

    public long getRegistros() {
        return registros;
    }

    public int getQualidade() {
        return qualidade;
    }

    public String getProcedencia() {
        return procedencia;
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
