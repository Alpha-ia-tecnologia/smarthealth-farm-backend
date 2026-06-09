package com.alphatech.cahosp.estoque.dto;

import java.util.List;

/**
 * Drill-down de uma posicao (RF-EST-03/06): a posicao, seus lotes e as movimentacoes recentes.
 */
public record PosicaoEstoqueDetalheResponse(
        PosicaoEstoqueResponse posicao,
        List<LoteResponse> lotes,
        List<MovimentacaoResponse> movimentacoes
) {
}
