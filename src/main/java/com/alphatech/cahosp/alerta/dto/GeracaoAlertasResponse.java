package com.alphatech.cahosp.alerta.dto;

/**
 * Resultado da regeneracao dos alertas pelo motor de regras (RF-ALE-01/02 — acao de Gestor):
 * quantos alertas de cada tipo foram gerados, quantos abertos foram renovados e o total ativo.
 */
public record GeracaoAlertasResponse(
        long desabastecimentoGerados,
        long vencimentoGerados,
        long abertosRenovados,
        long totalAtivo,
        String mensagem
) {
}
