package com.alphatech.cahosp.painel.dto;

import com.alphatech.cahosp.alerta.dto.AlertaResponse;
import com.alphatech.cahosp.recomendacao.dto.RecomendacaoResponse;

import java.util.List;

/**
 * Payload do painel operacional (RF-DASH-02): totais da rede, situacao por unidade, fila de
 * alertas ativos e recomendacoes ainda nao executadas.
 */
public record PainelOperacionalResponse(
        TotaisRedeResponse totais,
        List<ResumoUnidadeResponse> unidades,
        List<AlertaResponse> alertasAtivos,
        List<RecomendacaoResponse> recomendacoesAbertas
) {
}
