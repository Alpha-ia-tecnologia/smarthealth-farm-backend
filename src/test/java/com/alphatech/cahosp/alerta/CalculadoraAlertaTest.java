package com.alphatech.cahosp.alerta;

import com.alphatech.cahosp.alerta.dominio.Severidade;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes unitarios das regras puras de alerta (RF-ALE-01/02): bandas de severidade,
 * cobertura em dias e janela de vencimento.
 */
class CalculadoraAlertaTest {

    private final CalculadoraAlerta calc = new CalculadoraAlerta();

    @Test
    @DisplayName("Cobertura = saldo / consumo medio diario, com piso de 1 dia")
    void coberturaDias() {
        assertThat(calc.coberturaDias(100, 10)).isEqualTo(10);
        assertThat(calc.coberturaDias(45, 10)).isEqualTo(5);   // 4,5 -> Math.round arredonda p/ 5
        assertThat(calc.coberturaDias(3, 10)).isEqualTo(1);    // piso
        assertThat(calc.coberturaDias(0, 10)).isEqualTo(1);    // piso
    }

    @Test
    @DisplayName("Consumo zero nao divide por zero: usa o saldo como cobertura (piso 1)")
    void coberturaConsumoZero() {
        assertThat(calc.coberturaDias(8, 0)).isEqualTo(8);
        assertThat(calc.coberturaDias(0, 0)).isEqualTo(1);
    }

    @Test
    @DisplayName("Severidade por cobertura: <=5 Critico, <=10 Alto, senao Medio")
    void severidadePorCobertura() {
        assertThat(calc.severidadePorCobertura(1)).isEqualTo(Severidade.CRITICO);
        assertThat(calc.severidadePorCobertura(5)).isEqualTo(Severidade.CRITICO);
        assertThat(calc.severidadePorCobertura(6)).isEqualTo(Severidade.ALTO);
        assertThat(calc.severidadePorCobertura(10)).isEqualTo(Severidade.ALTO);
        assertThat(calc.severidadePorCobertura(11)).isEqualTo(Severidade.MEDIO);
        assertThat(calc.severidadePorCobertura(30)).isEqualTo(Severidade.MEDIO);
    }

    @Test
    @DisplayName("Severidade por vencimento: <=20 Critico, <=40 Alto, senao Medio")
    void severidadePorVencimento() {
        assertThat(calc.severidadePorVencimento(0)).isEqualTo(Severidade.CRITICO);
        assertThat(calc.severidadePorVencimento(20)).isEqualTo(Severidade.CRITICO);
        assertThat(calc.severidadePorVencimento(21)).isEqualTo(Severidade.ALTO);
        assertThat(calc.severidadePorVencimento(40)).isEqualTo(Severidade.ALTO);
        assertThat(calc.severidadePorVencimento(41)).isEqualTo(Severidade.MEDIO);
        assertThat(calc.severidadePorVencimento(60)).isEqualTo(Severidade.MEDIO);
    }

    @Test
    @DisplayName("Janela de vencimento: dispara ate 60 dias de antecedencia")
    void janelaVencimento() {
        assertThat(calc.dentroDaJanelaVencimento(60)).isTrue();
        assertThat(calc.dentroDaJanelaVencimento(0)).isTrue();
        assertThat(calc.dentroDaJanelaVencimento(61)).isFalse();
    }
}
