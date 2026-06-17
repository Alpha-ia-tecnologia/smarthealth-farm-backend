package com.alphatech.cahosp.unidade.dominio;

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
 * Unidade hospitalar da rede EMSERH (e a propria CAHOSP, marcada como {@code hub=true}).
 * RF-DAD-06 — variaveis de contexto da unidade (porte, leitos, conectividade, perfil demografico).
 *
 * <p>{@link #sigla} e unica e funciona como codigo de negocio (usado em logs, exportacoes e
 * pelo {@link com.alphatech.cahosp.unidade.UnidadeSeeder} para idempotencia).
 *
 * <p>{@link #hub} substitui o filtro fragil {@code id == "u-cahosp"} herdado do mock do front:
 * o hub logistico nao consome insumos diretamente, sendo excluido das listas operacionais.
 */
@Entity
@Table(name = "unidade")
@EntityListeners(AuditingEntityListener.class)
public class Unidade {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(nullable = false, length = 150)
    private String nome;

    @Column(nullable = false, length = 20, unique = true)
    private String sigla;

    @Column(nullable = false, length = 120)
    private String municipio;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Porte porte;

    @Column(nullable = false)
    private int leitos;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Conectividade conectividade;

    @Column(name = "perfil_demografico", nullable = false, length = 200)
    private String perfilDemografico;

    @Column(nullable = false)
    private boolean hub = false;

    @Column(nullable = false)
    private boolean ativo = true;

    @CreatedDate
    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @LastModifiedDate
    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    protected Unidade() {
        // JPA
    }

    public Unidade(String nome,
                   String sigla,
                   String municipio,
                   Porte porte,
                   int leitos,
                   Conectividade conectividade,
                   String perfilDemografico,
                   boolean hub) {
        this.nome = nome;
        this.sigla = sigla;
        this.municipio = municipio;
        this.porte = porte;
        this.leitos = leitos;
        this.conectividade = conectividade;
        this.perfilDemografico = perfilDemografico;
        this.hub = hub;
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

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getSigla() {
        return sigla;
    }

    public void setSigla(String sigla) {
        this.sigla = sigla;
    }

    public String getMunicipio() {
        return municipio;
    }

    public void setMunicipio(String municipio) {
        this.municipio = municipio;
    }

    public Porte getPorte() {
        return porte;
    }

    public void setPorte(Porte porte) {
        this.porte = porte;
    }

    public int getLeitos() {
        return leitos;
    }

    public void setLeitos(int leitos) {
        this.leitos = leitos;
    }

    public Conectividade getConectividade() {
        return conectividade;
    }

    public void setConectividade(Conectividade conectividade) {
        this.conectividade = conectividade;
    }

    public String getPerfilDemografico() {
        return perfilDemografico;
    }

    public void setPerfilDemografico(String perfilDemografico) {
        this.perfilDemografico = perfilDemografico;
    }

    public boolean isHub() {
        return hub;
    }

    public void setHub(boolean hub) {
        this.hub = hub;
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
