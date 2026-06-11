package com.alphatech.cahosp.alerta.dominio;

import com.alphatech.cahosp.comum.excecao.RegraNegocioException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

/**
 * Parametros/limiares que disparam os alertas (RF-ALE-03). Configuracao <strong>singleton</strong>
 * do sistema (uma unica linha, criada na migration V12) — o {@code GeradorAlerta} le estes valores
 * a cada regeneracao, entao alterar um limiar muda de fato o comportamento do motor.
 *
 * <ul>
 *   <li><strong>Desabastecimento:</strong> dispara quando o saldo fica abaixo de
 *       {@code percentualEstoqueMinimo}% do estoque minimo; a severidade segue a cobertura
 *       restante ({@code Crítico} ate {@code coberturaCriticaDias}, {@code Alto} ate
 *       {@code coberturaAltaDias}, senao {@code Médio}).</li>
 *   <li><strong>Vencimento:</strong> dispara para lotes com validade em ate
 *       {@code antecedenciaVencimentoDias}; severidade {@code Crítico} ate
 *       {@code vencimentoCriticoDias}, {@code Alto} ate {@code vencimentoAltoDias}.</li>
 * </ul>
 */
@Entity
@Table(name = "limiar_alerta")
@EntityListeners(AuditingEntityListener.class)
public class LimiarAlerta {

    /** Id fixo da linha de configuracao (singleton, semeada na migration V12). */
    public static final UUID ID_CONFIG = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    /** Dispara desabastecimento quando saldo < estoque minimo × este percentual (10–200%). */
    @Column(name = "percentual_estoque_minimo", nullable = false)
    private int percentualEstoqueMinimo;

    @Column(name = "cobertura_critica_dias", nullable = false)
    private int coberturaCriticaDias;

    @Column(name = "cobertura_alta_dias", nullable = false)
    private int coberturaAltaDias;

    /** Janela de antecedencia (dias) para um lote entrar no radar de vencimento. */
    @Column(name = "antecedencia_vencimento_dias", nullable = false)
    private int antecedenciaVencimentoDias;

    @Column(name = "vencimento_critico_dias", nullable = false)
    private int vencimentoCriticoDias;

    @Column(name = "vencimento_alto_dias", nullable = false)
    private int vencimentoAltoDias;

    @Column(name = "desabastecimento_ativo", nullable = false)
    private boolean desabastecimentoAtivo;

    @Column(name = "vencimento_ativo", nullable = false)
    private boolean vencimentoAtivo;

    @LastModifiedDate
    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    protected LimiarAlerta() {
        // JPA
    }

    /**
     * Cria a configuracao singleton em memoria (testes e cenarios sem banco), ja validando as
     * regras cruzadas. No runtime a linha vem da migration V12 — nao persista uma segunda.
     */
    public static LimiarAlerta criar(int percentualEstoqueMinimo,
                                     int coberturaCriticaDias, int coberturaAltaDias,
                                     int antecedenciaVencimentoDias,
                                     int vencimentoCriticoDias, int vencimentoAltoDias,
                                     boolean desabastecimentoAtivo, boolean vencimentoAtivo) {
        LimiarAlerta limiares = new LimiarAlerta();
        limiares.id = ID_CONFIG;
        limiares.atualizar(percentualEstoqueMinimo, coberturaCriticaDias, coberturaAltaDias,
                antecedenciaVencimentoDias, vencimentoCriticoDias, vencimentoAltoDias,
                desabastecimentoAtivo, vencimentoAtivo);
        return limiares;
    }

    /**
     * Atualiza todos os parametros de uma vez, validando a coerencia entre as bandas
     * (alto > critico; janela >= banda alta). Regras simples de faixa ficam no DTO (Bean
     * Validation); aqui ficam as regras <em>cruzadas</em>, que envolvem mais de um campo.
     */
    public void atualizar(int percentualEstoqueMinimo,
                          int coberturaCriticaDias, int coberturaAltaDias,
                          int antecedenciaVencimentoDias,
                          int vencimentoCriticoDias, int vencimentoAltoDias,
                          boolean desabastecimentoAtivo, boolean vencimentoAtivo) {
        if (coberturaAltaDias <= coberturaCriticaDias) {
            throw new RegraNegocioException(
                    "A banda 'Alto' da cobertura deve ser maior que a banda 'Crítico'.");
        }
        if (vencimentoAltoDias <= vencimentoCriticoDias) {
            throw new RegraNegocioException(
                    "A banda 'Alto' do vencimento deve ser maior que a banda 'Crítico'.");
        }
        if (antecedenciaVencimentoDias < vencimentoAltoDias) {
            throw new RegraNegocioException(
                    "A antecedência de vencimento deve cobrir a banda 'Alto' (janela >= banda).");
        }
        this.percentualEstoqueMinimo = percentualEstoqueMinimo;
        this.coberturaCriticaDias = coberturaCriticaDias;
        this.coberturaAltaDias = coberturaAltaDias;
        this.antecedenciaVencimentoDias = antecedenciaVencimentoDias;
        this.vencimentoCriticoDias = vencimentoCriticoDias;
        this.vencimentoAltoDias = vencimentoAltoDias;
        this.desabastecimentoAtivo = desabastecimentoAtivo;
        this.vencimentoAtivo = vencimentoAtivo;
    }

    public UUID getId() {
        return id;
    }

    public int getPercentualEstoqueMinimo() {
        return percentualEstoqueMinimo;
    }

    public int getCoberturaCriticaDias() {
        return coberturaCriticaDias;
    }

    public int getCoberturaAltaDias() {
        return coberturaAltaDias;
    }

    public int getAntecedenciaVencimentoDias() {
        return antecedenciaVencimentoDias;
    }

    public int getVencimentoCriticoDias() {
        return vencimentoCriticoDias;
    }

    public int getVencimentoAltoDias() {
        return vencimentoAltoDias;
    }

    public boolean isDesabastecimentoAtivo() {
        return desabastecimentoAtivo;
    }

    public boolean isVencimentoAtivo() {
        return vencimentoAtivo;
    }

    public Instant getAtualizadoEm() {
        return atualizadoEm;
    }
}
