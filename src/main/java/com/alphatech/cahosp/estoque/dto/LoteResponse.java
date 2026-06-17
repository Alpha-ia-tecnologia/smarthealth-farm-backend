package com.alphatech.cahosp.estoque.dto;

import com.alphatech.cahosp.estoque.dominio.Lote;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Lote com dias para vencer (derivado) para o controle de validade do front (RF-EST-02/03).
 */
public record LoteResponse(
        UUID id,
        UUID insumoId,
        String insumoNome,
        UUID unidadeId,
        String unidadeSigla,
        String numeroLote,
        LocalDate validade,
        long diasParaVencer,
        int quantidade,
        String fabricante
) {

    public static LoteResponse de(Lote lote, long diasParaVencer) {
        return new LoteResponse(
                lote.getId(),
                lote.getInsumo().getId(),
                lote.getInsumo().getNome(),
                lote.getUnidade().getId(),
                lote.getUnidade().getSigla(),
                lote.getNumeroLote(),
                lote.getValidade(),
                diasParaVencer,
                lote.getQuantidade(),
                lote.getFabricante());
    }
}
