package com.alphatech.cahosp.previsao;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Regras puras de previsao (RF-PRV-05). A meta de assertividade e MAPE &lt; 15% — relevante
 * sobretudo para os itens de maior criticidade.
 */
@Component
public class CalculadoraPrevisao {

    /** Meta de erro (MAPE) do projeto: abaixo de 15%. */
    public static final BigDecimal META_MAPE = new BigDecimal("15");

    /** {@code true} se o MAPE esta dentro da meta (&lt; 15%). */
    public boolean dentroDaMeta(BigDecimal mape) {
        return mape != null && mape.compareTo(META_MAPE) < 0;
    }
}
