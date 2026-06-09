package com.alphatech.cahosp.painel.dto;

import com.alphatech.cahosp.previsao.dto.PontoSerieResponse;

import java.util.List;
import java.util.UUID;

/**
 * Serie agregada de demanda (soma de todas as unidades) para o medicamento mais critico da rede
 * (RF-DASH/RF-PRV-02), base do grafico do dashboard gerencial.
 */
public record SerieAgregadaResponse(
        UUID medicamentoId,
        String medicamentoCodigo,
        String medicamentoNome,
        List<PontoSerieResponse> serie
) {
}
