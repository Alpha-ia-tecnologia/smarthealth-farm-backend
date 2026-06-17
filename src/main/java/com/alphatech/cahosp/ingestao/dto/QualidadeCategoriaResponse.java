package com.alphatech.cahosp.ingestao.dto;

import com.alphatech.cahosp.ingestao.dominio.GranularidadeDado;
import com.alphatech.cahosp.ingestao.dominio.QualidadeCategoria;
import com.alphatech.cahosp.insumo.dominio.CategoriaInsumo;

import java.util.UUID;

/**
 * Cartao de maturidade/qualidade por categoria (RF-DAD-04).
 */
public record QualidadeCategoriaResponse(
        UUID id,
        CategoriaInsumo categoria,
        int maturidade,
        int completude,
        int consistencia,
        GranularidadeDado granularidade,
        int lacunas
) {

    public static QualidadeCategoriaResponse de(QualidadeCategoria qualidade) {
        return new QualidadeCategoriaResponse(
                qualidade.getId(),
                qualidade.getCategoria(),
                qualidade.getMaturidade(),
                qualidade.getCompletude(),
                qualidade.getConsistencia(),
                qualidade.getGranularidade(),
                qualidade.getLacunas());
    }
}
