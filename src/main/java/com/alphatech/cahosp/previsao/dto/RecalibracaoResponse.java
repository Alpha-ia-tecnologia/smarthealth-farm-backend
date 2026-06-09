package com.alphatech.cahosp.previsao.dto;

import java.time.LocalDate;

/**
 * Resultado da recalibracao das previsoes (RF-PRV — acao de Gestor).
 */
public record RecalibracaoResponse(
        long recalibradas,
        LocalDate calibradoEm,
        String mensagem
) {
}
