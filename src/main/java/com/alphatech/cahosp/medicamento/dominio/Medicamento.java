package com.alphatech.cahosp.medicamento.dominio;

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
 * Medicamento ou insumo do catalogo da CAHOSP. Modelagem fiel ao tipo {@code Medicamento}
 * do front (src/types/index.ts).
 *
 * <p>{@link #codigo} e unico e funciona como codigo de negocio legivel (ex.: {@code MED-001}):
 * estavel para integracao com sistemas externos (EMSERH) e usado pelo
 * {@link com.alphatech.cahosp.medicamento.MedicamentoSeeder} para garantir idempotencia.
 */
@Entity
@Table(name = "medicamento")
@EntityListeners(AuditingEntityListener.class)
public class Medicamento {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(nullable = false, length = 30, unique = true)
    private String codigo;

    @Column(nullable = false, length = 200)
    private String nome;

    @Column(nullable = false, length = 80)
    private String apresentacao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private FamiliaTerapeutica familia;

    @Column(name = "unidade_medida", nullable = false, length = 20)
    private String unidadeMedida;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Criticidade criticidade;

    @Column(nullable = false)
    private boolean essencial;

    @Column(nullable = false)
    private boolean ativo = true;

    @CreatedDate
    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @LastModifiedDate
    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    protected Medicamento() {
        // JPA
    }

    public Medicamento(String codigo,
                       String nome,
                       String apresentacao,
                       FamiliaTerapeutica familia,
                       String unidadeMedida,
                       Criticidade criticidade,
                       boolean essencial) {
        this.codigo = codigo;
        this.nome = nome;
        this.apresentacao = apresentacao;
        this.familia = familia;
        this.unidadeMedida = unidadeMedida;
        this.criticidade = criticidade;
        this.essencial = essencial;
        this.ativo = true;
    }

    @PrePersist
    void aoCriar() {
        if (id == null) {
            id = UUID.randomUUID();
        }
    }

    public void ativar() {
        this.ativo = true;
    }

    public void desativar() {
        this.ativo = false;
    }

    public UUID getId() {
        return id;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getApresentacao() {
        return apresentacao;
    }

    public void setApresentacao(String apresentacao) {
        this.apresentacao = apresentacao;
    }

    public FamiliaTerapeutica getFamilia() {
        return familia;
    }

    public void setFamilia(FamiliaTerapeutica familia) {
        this.familia = familia;
    }

    public String getUnidadeMedida() {
        return unidadeMedida;
    }

    public void setUnidadeMedida(String unidadeMedida) {
        this.unidadeMedida = unidadeMedida;
    }

    public Criticidade getCriticidade() {
        return criticidade;
    }

    public void setCriticidade(Criticidade criticidade) {
        this.criticidade = criticidade;
    }

    public boolean isEssencial() {
        return essencial;
    }

    public void setEssencial(boolean essencial) {
        this.essencial = essencial;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public Instant getAtualizadoEm() {
        return atualizadoEm;
    }
}
