package com.alphatech.cahosp.indicador;

import com.alphatech.cahosp.indicador.dominio.IndicadorMeta;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Regras puras dos indicadores (RF-IND) — sem dependencia de banco, faceis de testar.
 * Espelham {@code progresso} e {@code atingiu} da IndicadoresPage do front.
 */
@Component
public class CalculadoraIndicador {

    /** Teto do progresso exibido (ate 140% do caminho ate a meta), conforme o front. */
    private static final int PROGRESSO_MAXIMO = 140;

    /**
     * Percentual do caminho percorrido entre a linha de base e a meta
     * ({@code (baseline - atual) / (baseline - meta) * 100}), limitado a [0, 140]. RF-IND-01.
     * Se {@code baseline == meta}, considera 100%.
     */
    public int progresso(BigDecimal baseline, BigDecimal atual, BigDecimal meta) {
        if (baseline.compareTo(meta) == 0) {
            return 100;
        }
        double percorrido = (baseline.doubleValue() - atual.doubleValue())
                / (baseline.doubleValue() - meta.doubleValue()) * 100.0;
        return (int) Math.max(0, Math.min(PROGRESSO_MAXIMO, Math.round(percorrido)));
    }

    /**
     * A meta foi atingida? Para indicadores "quanto menor melhor", {@code atual <= meta};
     * caso contrario, {@code atual >= meta}. RF-IND-04.
     */
    public boolean atingiu(BigDecimal atual, BigDecimal meta, boolean melhorMenor) {
        return melhorMenor ? atual.compareTo(meta) <= 0 : atual.compareTo(meta) >= 0;
    }

    /**
     * Variacao percentual do valor atual frente a linha de base
     * ({@code (atual - baseline) / baseline * 100}). Base do comparativo piloto x sistema atual
     * (RF-IND-06). Retorna {@code null} quando a linha de base e zero (variacao indefinida).
     */
    public Integer variacaoPct(BigDecimal baseline, BigDecimal atual) {
        if (baseline.signum() == 0) {
            return null;
        }
        double variacao = (atual.doubleValue() - baseline.doubleValue())
                / baseline.doubleValue() * 100.0;
        return (int) Math.round(variacao);
    }

    /** Conveniencia: progresso a partir da entidade. */
    public int progresso(IndicadorMeta i) {
        return progresso(i.getBaseline(), i.getAtual(), i.getMeta());
    }

    /** Conveniencia: meta atingida a partir da entidade. */
    public boolean atingiu(IndicadorMeta i) {
        return atingiu(i.getAtual(), i.getMeta(), i.isMelhorMenor());
    }

    /** Conveniencia: variacao a partir da entidade. */
    public Integer variacaoPct(IndicadorMeta i) {
        return variacaoPct(i.getBaseline(), i.getAtual());
    }
}
