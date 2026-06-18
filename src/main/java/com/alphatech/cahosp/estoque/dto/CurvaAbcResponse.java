package com.alphatech.cahosp.estoque.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Curva ABC dos insumos por valor de consumo (RF-EST): a lista ordenada de itens (com participacao
 * e acumulado, para o grafico de Pareto) e o resumo por classe A/B/C (para os cartoes-resumo).
 */
public record CurvaAbcResponse(List<Item> itens, List<ResumoClasse> resumo) {

    /** Um insumo na curva: valor de consumo, participacao/acumulado (%) e a classe A/B/C. */
    public record Item(
            UUID insumoId,
            String insumoCodigo,
            String insumoNome,
            long consumoMedioDiario,
            BigDecimal custoUnitario,
            BigDecimal valorConsumo,
            BigDecimal participacaoPct,
            BigDecimal acumuladoPct,
            String classe) {}

    /** Consolidado de uma classe: nº de itens, valor e os percentuais de itens e de valor. */
    public record ResumoClasse(
            String classe,
            int itens,
            BigDecimal valor,
            BigDecimal itensPct,
            BigDecimal valorPct) {}
}
