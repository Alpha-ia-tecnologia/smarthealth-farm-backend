package com.alphatech.cahosp.estoque.dto;

import com.alphatech.cahosp.estoque.dominio.Movimentacao;
import com.alphatech.cahosp.estoque.dominio.TipoMovimentacao;

import java.time.Instant;
import java.util.UUID;

/**
 * Lancamento do livro-razao para o historico/auditoria do front (RF-EST-06).
 */
public record MovimentacaoResponse(
        UUID id,
        UUID loteId,
        String numeroLote,
        UUID insumoId,
        String insumoNome,
        UUID unidadeId,
        String unidadeSigla,
        TipoMovimentacao tipo,
        int quantidade,
        Instant dataHora,
        String responsavel,
        String documento
) {

    public static MovimentacaoResponse de(Movimentacao m) {
        return new MovimentacaoResponse(
                m.getId(),
                m.getLote().getId(),
                m.getLote().getNumeroLote(),
                m.getInsumo().getId(),
                m.getInsumo().getNome(),
                m.getUnidade().getId(),
                m.getUnidade().getSigla(),
                m.getTipo(),
                m.getQuantidade(),
                m.getDataHora(),
                m.getResponsavel(),
                m.getDocumento());
    }
}
