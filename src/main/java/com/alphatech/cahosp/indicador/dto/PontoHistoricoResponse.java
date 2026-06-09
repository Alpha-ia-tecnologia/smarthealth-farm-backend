package com.alphatech.cahosp.indicador.dto;

import com.alphatech.cahosp.indicador.dominio.PontoHistorico;

import java.math.BigDecimal;

/**
 * Ponto da serie de historico de um indicador (RF-IND-05): periodo mensal e valor medido.
 */
public record PontoHistoricoResponse(
        String periodo,
        BigDecimal valor
) {

    public static PontoHistoricoResponse de(PontoHistorico p) {
        return new PontoHistoricoResponse(p.getPeriodo(), p.getValor());
    }
}
