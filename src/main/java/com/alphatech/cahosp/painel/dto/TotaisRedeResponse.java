package com.alphatech.cahosp.painel.dto;

import java.math.BigDecimal;

/**
 * Totais consolidados da rede (RF-DASH-01), espelhando o objeto {@code totais} do front:
 * contagem de catalogo, alertas por situacao, recomendacoes pendentes, economia potencial,
 * itens criticos e lotes proximos do vencimento.
 */
public record TotaisRedeResponse(
        long medicamentos,
        long unidades,
        long alertasAbertos,
        long alertasDesabastecimento,
        long alertasVencimento,
        long recomendacoesPendentes,
        BigDecimal economiaPotencial,
        long itensCriticos,
        long lotesProximosVencimento
) {
}
