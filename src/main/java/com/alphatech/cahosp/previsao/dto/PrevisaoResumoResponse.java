package com.alphatech.cahosp.previsao.dto;

import com.alphatech.cahosp.medicamento.dominio.Criticidade;
import com.alphatech.cahosp.previsao.dominio.Drift;
import com.alphatech.cahosp.previsao.dominio.Previsao;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Linha da tabela de previsoes do front (RF-PRV-01): medicamento (com criticidade), unidade,
 * MAPE, modelo/versao e drift.
 */
public record PrevisaoResumoResponse(
        UUID id,
        UUID medicamentoId,
        String medicamentoCodigo,
        String medicamentoNome,
        Criticidade criticidade,
        UUID unidadeId,
        String unidadeSigla,
        String unidadeNome,
        int horizonteMeses,
        BigDecimal mape,
        String modelo,
        String versaoModelo,
        Drift drift,
        LocalDate calibradoEm
) {

    public static PrevisaoResumoResponse de(Previsao p) {
        return new PrevisaoResumoResponse(
                p.getId(),
                p.getMedicamento().getId(),
                p.getMedicamento().getCodigo(),
                p.getMedicamento().getNome(),
                p.getMedicamento().getCriticidade(),
                p.getUnidade().getId(),
                p.getUnidade().getSigla(),
                p.getUnidade().getNome(),
                p.getHorizonteMeses(),
                p.getMape(),
                p.getModelo(),
                p.getVersaoModelo(),
                p.getDrift(),
                p.getCalibradoEm());
    }
}
