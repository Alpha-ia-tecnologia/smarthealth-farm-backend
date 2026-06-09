package com.alphatech.cahosp.ingestao.dto;

import com.alphatech.cahosp.ingestao.dominio.GranularidadeDado;
import com.alphatech.cahosp.ingestao.dominio.QualidadeFamilia;
import com.alphatech.cahosp.medicamento.dominio.FamiliaTerapeutica;

import java.util.UUID;

/**
 * Cartao de maturidade/qualidade por familia terapeutica (RF-DAD-04).
 */
public record QualidadeFamiliaResponse(
        UUID id,
        FamiliaTerapeutica familia,
        int maturidade,
        int completude,
        int consistencia,
        GranularidadeDado granularidade,
        int lacunas
) {

    public static QualidadeFamiliaResponse de(QualidadeFamilia qualidade) {
        return new QualidadeFamiliaResponse(
                qualidade.getId(),
                qualidade.getFamilia(),
                qualidade.getMaturidade(),
                qualidade.getCompletude(),
                qualidade.getConsistencia(),
                qualidade.getGranularidade(),
                qualidade.getLacunas());
    }
}
