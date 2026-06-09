package com.alphatech.cahosp.recomendacao.dto;

/**
 * Resultado da regeneracao das recomendacoes pelo motor de regras (RF-REC-01 — acao de Gestor):
 * quantas de cada tipo foram geradas, quantas pendentes foram renovadas e o total ativo.
 */
public record GeracaoRecomendacoesResponse(
        long reposicaoGeradas,
        long redistribuicaoGeradas,
        long pendentesRenovadas,
        long totalAtivo,
        String mensagem
) {
}
