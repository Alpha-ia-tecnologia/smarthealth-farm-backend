package com.alphatech.cahosp.painel;

import com.alphatech.cahosp.estoque.dominio.StatusEstoque;
import org.springframework.stereotype.Component;

/**
 * Regras puras de agregacao do painel (RF-DASH-01/02) — sem dependencia de banco, faceis de
 * testar. Espelham {@code resumoUnidade} e os limiares de cobertura/situacao do front
 * (DashboardPage/OperacionalPage).
 *
 * <p>Reutiliza o token {@link StatusEstoque} ({@code ok}/{@code atencao}/{@code critico}) como
 * status agregado, alinhado aos badges da UI.
 */
@Component
public class CalculadoraPainel {

    /** Cobertura do dashboard: % de itens com estoque saudavel (status OK) sobre o total. */
    public int coberturaPercentual(int itensOk, int totalItens) {
        if (totalItens <= 0) {
            return 0;
        }
        return Math.round(itensOk * 100f / totalItens);
    }

    /**
     * Status da cobertura por unidade (DashboardPage): {@code critico} abaixo de 60%,
     * {@code atencao} abaixo de 80%, senao {@code ok}. RF-DASH-01.
     */
    public StatusEstoque statusCobertura(int coberturaPercentual) {
        if (coberturaPercentual < 60) {
            return StatusEstoque.CRITICO;
        }
        if (coberturaPercentual < 80) {
            return StatusEstoque.ATENCAO;
        }
        return StatusEstoque.OK;
    }

    /**
     * Status operacional da unidade pela quantidade de itens criticos (OperacionalPage):
     * {@code critico} com mais de 3, {@code atencao} com 1 a 3, senao {@code ok}. RF-DASH-02.
     */
    public StatusEstoque statusUnidade(int itensCriticos) {
        if (itensCriticos > 3) {
            return StatusEstoque.CRITICO;
        }
        if (itensCriticos > 0) {
            return StatusEstoque.ATENCAO;
        }
        return StatusEstoque.OK;
    }
}
