package com.alphatech.cahosp.previsao.dto;

import java.util.List;

/**
 * Detalhe de uma previsao: o resumo e a serie temporal completa (RF-PRV-02).
 */
public record PrevisaoDetalheResponse(
        PrevisaoResumoResponse previsao,
        List<PontoSerieResponse> serie
) {
}
