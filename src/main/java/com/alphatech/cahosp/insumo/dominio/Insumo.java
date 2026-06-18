package com.alphatech.cahosp.insumo.dominio;

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
 * Insumo do catalogo da CAHOSP. Modelagem fiel ao tipo {@code Insumo}
 * do front (src/types/index.ts).
 *
 * <p>{@link #codigo} e unico e funciona como codigo de negocio legivel (ex.: {@code INS-001}):
 * estavel para integracao com sistemas externos (EMSERH) e usado pelo
 * {@link com.alphatech.cahosp.insumo.InsumoSeeder} para garantir idempotencia.
 */
@Entity
@Table(name = "insumo")
@EntityListeners(AuditingEntityListener.class)
public class Insumo {

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
    private CategoriaInsumo categoria;

    @Column(name = "unidade_medida", nullable = false, length = 20)
    private String unidadeMedida;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Criticidade criticidade;

    @Column(nullable = false)
    private boolean essencial;

    // Custo unitario (R$) na unidade de medida do insumo — base do valor de consumo na Curva ABC
    // (RF-EST). Pode ser nulo (insumo recem-cadastrado sem custo informado); o seeder preenche os
    // do catalogo demo.
    @Column(name = "custo_unitario", precision = 12, scale = 2)
    private BigDecimal custoUnitario;

    @Column(nullable = false)
    private boolean ativo = true;

    @CreatedDate
    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @LastModifiedDate
    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    protected Insumo() {
        // JPA
    }

    public Insumo(String codigo,
                       String nome,
                       String apresentacao,
                       CategoriaInsumo categoria,
                       String unidadeMedida,
                       Criticidade criticidade,
                       boolean essencial) {
        this.codigo = codigo;
        this.nome = nome;
        this.apresentacao = apresentacao;
        this.categoria = categoria;
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

    public CategoriaInsumo getCategoria() {
        return categoria;
    }

    public void setCategoria(CategoriaInsumo categoria) {
        this.categoria = categoria;
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

    public BigDecimal getCustoUnitario() {
        return custoUnitario;
    }

    public void setCustoUnitario(BigDecimal custoUnitario) {
        this.custoUnitario = custoUnitario;
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
