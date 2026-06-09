package com.alphatech.cahosp.recomendacao.dto;

import java.math.BigDecimal;

/**
 * KPIs do painel de recomendacoes (RF-REC-01/02/03/05), espelhando os cards do front:
 * <ul>
 *   <li>{@code pendentes} — "Recomendações pendentes";</li>
 *   <li>{@code economiaPotencial} — soma de {@code economiaEstimada} em R$ ("Economia potencial");</li>
 *   <li>{@code geradasPorIA} — recomendacoes com motor de Aprendizado de Maquina ("Geradas por IA");</li>
 *   <li>{@code taxaAdesao} — % de recomendacoes nao pendentes (aprovadas + executadas).</li>
 * </ul>
 */
public record ResumoRecomendacoesResponse(
        long pendentes,
        long aprovadas,
        long executadas,
        BigDecimal economiaPotencial,
        long geradasPorIA,
        int taxaAdesao,
        long total
) {
}
