package com.alphatech.cahosp.previsao;

/**
 * Projecao da serie agregada de previsao por periodo (soma de realizado/previsto e bandas de
 * todas as unidades de um medicamento). Alimenta o grafico consolidado do dashboard (RF-DASH/PRV).
 * Valores {@code null} quando a soma e nula no periodo (ex.: realizado nos meses de projecao).
 */
public interface SeriePeriodoAgregada {

    String getPeriodo();

    Long getRealizado();

    Long getPrevisto();

    Long getLimiteInferior();

    Long getLimiteSuperior();
}
