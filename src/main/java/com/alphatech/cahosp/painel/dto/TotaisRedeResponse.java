package com.alphatech.cahosp.painel.dto;

import java.math.BigDecimal;

/**
 * Totais consolidados da rede (RF-DASH-01), espelhando o objeto {@code totais} do front:
 * contagem de catalogo, alertas por situacao, recomendacoes pendentes, economia potencial,
 * itens criticos e lotes proximos do vencimento.
 *
 * <p>{@code alertasAbertos} conta apenas o status ABERTO; {@code alertasAtivos} conta os nao
 * resolvidos (ABERTO + EM_TRATAMENTO) — a mesma metrica "ativos" do painel de alertas, para que
 * o dashboard e a tela de alertas exibam o mesmo numero.
 */
public record TotaisRedeResponse(
        long medicamentos,
        long unidades,
        long alertasAbertos,
        long alertasAtivos,
        long alertasDesabastecimento,
        long alertasVencimento,
        long recomendacoesPendentes,
        BigDecimal economiaPotencial,
        long itensCriticos,
        long lotesProximosVencimento
) {
}
