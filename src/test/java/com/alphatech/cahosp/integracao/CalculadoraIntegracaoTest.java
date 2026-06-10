package com.alphatech.cahosp.integracao;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes unitarios da latencia media da integracao (RF-INT-02): considera apenas conexoes
 * ativas (latencia &gt; 0).
 */
class CalculadoraIntegracaoTest {

    private final CalculadoraIntegracao calc = new CalculadoraIntegracao();

    @Test
    @DisplayName("Latencia media ignora integracoes indisponiveis (latencia 0)")
    void latenciaMediaIgnoraZeradas() {
        // (142 + 1840 + 410 + 220) / 4 = 653; o 0 (indisponivel) e descartado
        assertThat(calc.latenciaMediaMs(List.of(142, 1840, 410, 220, 0))).isEqualTo(653);
    }

    @Test
    @DisplayName("Sem latencias positivas => 0")
    void semLatenciasPositivas() {
        assertThat(calc.latenciaMediaMs(List.of(0, 0))).isZero();
        assertThat(calc.latenciaMediaMs(List.of())).isZero();
        assertThat(calc.latenciaMediaMs(null)).isZero();
    }

    @Test
    @DisplayName("Arredonda a media para o inteiro mais proximo")
    void arredonda() {
        // (100 + 101) / 2 = 100,5 -> 101 (Math.round)
        assertThat(calc.latenciaMediaMs(List.of(100, 101))).isEqualTo(101);
    }
}
