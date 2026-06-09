package com.alphatech.cahosp.indicador;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes unitarios das derivacoes dos indicadores (RF-IND-01/04/06): progresso, meta atingida
 * e variacao.
 */
class CalculadoraIndicadorTest {

    private final CalculadoraIndicador calc = new CalculadoraIndicador();

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }

    @Test
    @DisplayName("Progresso = caminho percorrido da base ate a meta, limitado a [0, 140]")
    void progresso() {
        // base 18.4, atual 11.2, meta 11.96: (18.4-11.2)/(18.4-11.96)*100 = 111,8 -> 112
        assertThat(calc.progresso(bd("18.4"), bd("11.2"), bd("11.96"))).isEqualTo(112);
        // ainda na base => 0%
        assertThat(calc.progresso(bd("18.4"), bd("18.4"), bd("11.96"))).isEqualTo(0);
        // muito alem da meta => teto 140
        assertThat(calc.progresso(bd("100"), bd("0"), bd("90"))).isEqualTo(140);
    }

    @Test
    @DisplayName("Progresso com baseline igual a meta => 100%")
    void progressoBaseIgualMeta() {
        assertThat(calc.progresso(bd("10"), bd("10"), bd("10"))).isEqualTo(100);
    }

    @Test
    @DisplayName("Meta atingida: 'menor melhor' => atual <= meta; senao atual >= meta")
    void atingiu() {
        assertThat(calc.atingiu(bd("11.2"), bd("11.96"), true)).isTrue();
        assertThat(calc.atingiu(bd("12.5"), bd("11.96"), true)).isFalse();
        // "maior melhor" (ex.: desabastecimentos evitados)
        assertThat(calc.atingiu(bd("147"), bd("120"), false)).isTrue();
        assertThat(calc.atingiu(bd("100"), bd("120"), false)).isFalse();
    }

    @Test
    @DisplayName("Variacao percentual atual x base; null quando a base e zero")
    void variacaoPct() {
        // (812 - 1240)/1240 * 100 = -34,52% -> Math.round = -35
        assertThat(calc.variacaoPct(bd("1240"), bd("812"))).isEqualTo(-35);
        // (11.2 - 18.4)/18.4 * 100 = -39,13% -> -39
        assertThat(calc.variacaoPct(bd("18.4"), bd("11.2"))).isEqualTo(-39);
        assertThat(calc.variacaoPct(bd("0"), bd("147"))).isNull();
    }
}
