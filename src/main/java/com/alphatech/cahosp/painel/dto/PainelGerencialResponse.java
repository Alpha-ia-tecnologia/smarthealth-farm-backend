package com.alphatech.cahosp.painel.dto;

import com.alphatech.cahosp.alerta.dto.AlertaResponse;
import com.alphatech.cahosp.recomendacao.dto.RecomendacaoResponse;

import java.util.List;

/**
 * Payload do dashboard gerencial (RF-DASH-01): totais da rede, cobertura por unidade, serie
 * agregada de previsao, alertas recentes e recomendacoes pendentes de maior impacto.
 */
public record PainelGerencialResponse(
        TotaisRedeResponse totais,
        List<CoberturaUnidadeResponse> coberturaPorUnidade,
        SerieAgregadaResponse serieAgregada,
        List<AlertaResponse> alertasRecentes,
        List<RecomendacaoResponse> recomendacoesPendentes
) {
}
