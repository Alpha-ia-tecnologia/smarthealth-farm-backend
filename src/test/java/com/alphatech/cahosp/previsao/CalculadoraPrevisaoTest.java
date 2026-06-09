package com.alphatech.cahosp.previsao;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes unitarios da meta de MAPE (RF-PRV-05).
 */
class CalculadoraPrevisaoTest {

    private final CalculadoraPrevisao calc = new CalculadoraPrevisao();

    @Test
    @DisplayName("MAPE abaixo de 15% esta dentro da meta")
    void dentroDaMeta() {
        assertThat(calc.dentroDaMeta(new BigDecimal("9.00"))).isTrue();
        assertThat(calc.dentroDaMeta(new BigDecimal("14.99"))).isTrue();
    }

    @Test
    @DisplayName("MAPE igual ou acima de 15% esta fora da meta")
    void foraDaMeta() {
        assertThat(calc.dentroDaMeta(new BigDecimal("15.00"))).isFalse();
        assertThat(calc.dentroDaMeta(new BigDecimal("22.40"))).isFalse();
    }

    @Test
    @DisplayName("MAPE nulo nao conta como dentro da meta")
    void nulo() {
        assertThat(calc.dentroDaMeta(null)).isFalse();
    }
}
