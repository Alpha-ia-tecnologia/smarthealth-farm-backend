package com.alphatech.cahosp.ingestao.dto;

/**
 * KPIs do painel de ingestao (RF-DAD-01/02/03/04): volume total, fontes sincronizadas,
 * qualidade media e status da anonimizacao LGPD.
 */
public record ResumoIngestaoResponse(
        long registrosIngeridos,
        long totalFontes,
        long fontesSincronizadas,
        int qualidadeMedia,
        boolean anonimizacaoAtiva
) {
}
