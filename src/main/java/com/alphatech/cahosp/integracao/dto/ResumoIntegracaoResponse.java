package com.alphatech.cahosp.integracao.dto;

/**
 * KPIs do painel de integracao (RF-INT-01/02/05/06): integracoes operacionais sobre o total,
 * latencia media, volume em buffer offline e quantidade de provedores de IA registrados.
 */
public record ResumoIntegracaoResponse(
        long operacionais,
        long totalIntegracoes,
        int latenciaMediaMs,
        long registrosBuffer,
        long provedoresIa
) {
}
