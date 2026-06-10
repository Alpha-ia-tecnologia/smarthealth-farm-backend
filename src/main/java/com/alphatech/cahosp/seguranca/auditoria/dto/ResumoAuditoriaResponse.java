package com.alphatech.cahosp.seguranca.auditoria.dto;

import java.time.Instant;

/**
 * KPIs do painel de Seguranca/Auditoria (RF-SEG-02/05): total de eventos auditados, decisoes
 * assistidas por IA, eventos com base legal LGPD registrada e o instante da ultima atividade.
 *
 * @param total              total de eventos na trilha
 * @param assistidosPorIa    eventos cuja acao foi assistida por IA (RF-SEG-02/04)
 * @param comBaseLegal       eventos com base legal LGPD registrada (RF-SEG-03)
 * @param ultimaAtividade    instante do evento mais recente (nulo se a trilha estiver vazia)
 */
public record ResumoAuditoriaResponse(
        long total,
        long assistidosPorIa,
        long comBaseLegal,
        Instant ultimaAtividade
) {
}
