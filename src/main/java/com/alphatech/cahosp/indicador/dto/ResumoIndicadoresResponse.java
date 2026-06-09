package com.alphatech.cahosp.indicador.dto;

/**
 * KPIs do painel de indicadores (RF-IND-04/05): total monitorado, metas atingidas e indicadores
 * ainda em progresso.
 */
public record ResumoIndicadoresResponse(
        long total,
        long atingidas,
        long emProgresso
) {
}
