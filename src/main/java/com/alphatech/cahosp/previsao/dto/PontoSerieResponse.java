package com.alphatech.cahosp.previsao.dto;

import com.alphatech.cahosp.previsao.dominio.PontoSerie;

/**
 * Ponto da serie temporal (RF-PRV-02): realizado/previsto por periodo, com bandas opcionais.
 */
public record PontoSerieResponse(
        String periodo,
        Integer realizado,
        Integer previsto,
        Integer limiteInferior,
        Integer limiteSuperior
) {

    public static PontoSerieResponse de(PontoSerie p) {
        return new PontoSerieResponse(p.getPeriodo(), p.getRealizado(), p.getPrevisto(),
                p.getLimiteInferior(), p.getLimiteSuperior());
    }
}
