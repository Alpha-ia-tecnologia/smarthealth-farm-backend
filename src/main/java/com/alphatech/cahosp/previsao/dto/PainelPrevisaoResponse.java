package com.alphatech.cahosp.previsao.dto;

import java.math.BigDecimal;

/**
 * KPIs do painel de previsao (RF-PRV-04/05/06): erro medio (MAPE), itens criticos dentro da
 * meta, total de previsoes ativas e itens com desvio (drift degradado).
 */
public record PainelPrevisaoResponse(
        BigDecimal mapeMedio,
        long criticosNaMeta,
        long totalCriticos,
        long previsoesAtivas,
        long itensComDesvio
) {
}
