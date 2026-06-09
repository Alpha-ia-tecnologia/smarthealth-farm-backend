package com.alphatech.cahosp.recomendacao.dto;

import com.alphatech.cahosp.recomendacao.dominio.OrigemMotor;
import com.alphatech.cahosp.recomendacao.dominio.Prioridade;
import com.alphatech.cahosp.recomendacao.dominio.Recomendacao;
import com.alphatech.cahosp.recomendacao.dominio.StatusRecomendacao;
import com.alphatech.cahosp.recomendacao.dominio.TipoRecomendacao;
import com.alphatech.cahosp.unidade.dominio.Unidade;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Cartao de recomendacao do front (RF-REC-01/02/03/04): tipo, medicamento, origem -> destino,
 * quantidade, justificativa, motor, prioridade, economia estimada (R$) e status.
 * {@code unidadeOrigem*} so vem em redistribuicao.
 */
public record RecomendacaoResponse(
        UUID id,
        TipoRecomendacao tipo,
        UUID medicamentoId,
        String medicamentoCodigo,
        String medicamentoNome,
        UUID unidadeDestinoId,
        String unidadeDestinoSigla,
        String unidadeDestinoNome,
        UUID unidadeOrigemId,
        String unidadeOrigemSigla,
        String unidadeOrigemNome,
        int quantidade,
        String justificativa,
        OrigemMotor origemMotor,
        Prioridade prioridade,
        BigDecimal economiaEstimada,
        StatusRecomendacao status,
        Instant criadoEm
) {

    public static RecomendacaoResponse de(Recomendacao r) {
        Unidade origem = r.getUnidadeOrigem();
        return new RecomendacaoResponse(
                r.getId(),
                r.getTipo(),
                r.getMedicamento().getId(),
                r.getMedicamento().getCodigo(),
                r.getMedicamento().getNome(),
                r.getUnidadeDestino().getId(),
                r.getUnidadeDestino().getSigla(),
                r.getUnidadeDestino().getNome(),
                origem == null ? null : origem.getId(),
                origem == null ? null : origem.getSigla(),
                origem == null ? null : origem.getNome(),
                r.getQuantidade(),
                r.getJustificativa(),
                r.getOrigemMotor(),
                r.getPrioridade(),
                r.getEconomiaEstimada(),
                r.getStatus(),
                r.getCriadoEm());
    }
}
