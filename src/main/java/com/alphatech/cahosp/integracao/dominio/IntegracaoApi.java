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

import java.time.Instant;
import java.util.UUID;

/**
 * Integracao com um sistema externo da EMSERH (RF-INT-01/02/04/05): API versionada, situacao,
 * latencia, modo de operacao (online/offline-buffer/reconciliando) e volume em buffer offline.
 * {@link #codigo} e o identificador de negocio legivel (ex.: {@code api-farmaweb}), usado pelo
 * seeder para idempotencia.
 */
@Entity
@Table(name = "integracao_api")
@EntityListeners(AuditingEntityListener.class)
public class IntegracaoApi {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(nullable = false, length = 40, unique = true)
    private String codigo;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(nullable = false, length = 20)
    private String versao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusIntegracao status;

    @Column(name = "latencia_ms", nullable = false)
    private int latenciaMs;

    @Column(name = "ultima_sync", nullable = false)
    private Instant ultimaSync;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ModoIntegracao modo;

    @Column(name = "registros_buffer", nullable = false)
    private int registrosBuffer;

    @Column(nullable = false)
    private int ordem;

    @CreatedDate
    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @LastModifiedDate
    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    protected IntegracaoApi() {
    }

    public IntegracaoApi(String codigo, String nome, String versao, StatusIntegracao status,
                         int latenciaMs, Instant ultimaSync, ModoIntegracao modo,
                         int registrosBuffer, int ordem) {
        this.codigo = codigo;
        this.nome = nome;
        this.versao = versao;
        this.status = status;
        this.latenciaMs = latenciaMs;
        this.ultimaSync = ultimaSync;
        this.modo = modo;
        this.registrosBuffer = registrosBuffer;
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

    public String getVersao() {
        return versao;
    }

    public StatusIntegracao getStatus() {
        return status;
    }

    public int getLatenciaMs() {
        return latenciaMs;
    }

    public Instant getUltimaSync() {
        return ultimaSync;
    }

    public ModoIntegracao getModo() {
        return modo;
    }

    public int getRegistrosBuffer() {
        return registrosBuffer;
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
