package com.alphatech.cahosp.estoque.dto;

import com.alphatech.cahosp.estoque.dominio.PosicaoEstoque;
import com.alphatech.cahosp.estoque.dominio.StatusEstoque;

import java.util.UUID;

/**
 * Posicao de estoque para a tabela do front (RF-EST-01/04). O {@code status} e derivado.
 */
public record PosicaoEstoqueResponse(
        UUID id,
        UUID insumoId,
        String insumoCodigo,
        String insumoNome,
        UUID unidadeId,
        String unidadeSigla,
        String unidadeNome,
        int quantidade,
        int nivelCritico,
        int estoqueMaximo,
        int consumoMedioDiario,
        int tempoMedioRessuprimentoDias,
        StatusEstoque status
) {

    public static PosicaoEstoqueResponse de(PosicaoEstoque p, StatusEstoque status) {
        return new PosicaoEstoqueResponse(
                p.getId(),
                p.getInsumo().getId(),
                p.getInsumo().getCodigo(),
                p.getInsumo().getNome(),
                p.getUnidade().getId(),
                p.getUnidade().getSigla(),
                p.getUnidade().getNome(),
                p.getQuantidade(),
                p.getNivelCritico(),
                p.getEstoqueMaximo(),
                p.getConsumoMedioDiario(),
                p.getTempoMedioRessuprimentoDias(),
                status);
    }
}
