package com.alphatech.cahosp.painel;

import com.alphatech.cahosp.estoque.dominio.StatusEstoque;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes unitarios das regras puras de agregacao do painel (sem Spring, sem Docker). RF-DASH-01/02.
 */
class CalculadoraPainelTest {

    private final CalculadoraPainel calc = new CalculadoraPainel();

    @Test
    @DisplayName("coberturaPercentual arredonda % de itens OK sobre o total")
    void coberturaPercentual() {
        assertThat(calc.coberturaPercentual(0, 0)).isZero();
        assertThat(calc.coberturaPercentual(3, 4)).isEqualTo(75);
        assertThat(calc.coberturaPercentual(8, 10)).isEqualTo(80);
    }

    @Test
    @DisplayName("statusCobertura aplica limiares 60% e 80%")
    void statusCobertura() {
        assertThat(calc.statusCobertura(59)).isEqualTo(StatusEstoque.CRITICO);
        assertThat(calc.statusCobertura(60)).isEqualTo(StatusEstoque.ATENCAO);
        assertThat(calc.statusCobertura(79)).isEqualTo(StatusEstoque.ATENCAO);
        assertThat(calc.statusCobertura(80)).isEqualTo(StatusEstoque.OK);
        assertThat(calc.statusCobertura(100)).isEqualTo(StatusEstoque.OK);
    }

    @Test
    @DisplayName("statusUnidade aplica limiares de itens criticos (1-3 atencao, >3 critico)")
    void statusUnidade() {
        assertThat(calc.statusUnidade(0)).isEqualTo(StatusEstoque.OK);
        assertThat(calc.statusUnidade(1)).isEqualTo(StatusEstoque.ATENCAO);
        assertThat(calc.statusUnidade(3)).isEqualTo(StatusEstoque.ATENCAO);
        assertThat(calc.statusUnidade(4)).isEqualTo(StatusEstoque.CRITICO);
    }
}
