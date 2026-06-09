package com.alphatech.cahosp.estoque;

import com.alphatech.cahosp.estoque.dominio.StatusEstoque;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes unitarios das regras puras de estoque (sem Spring, sem Docker). RF-EST-02/04.
 */
class CalculadoraEstoqueTest {

    private final CalculadoraEstoque calc = new CalculadoraEstoque();

    @Test
    @DisplayName("Abaixo do nivel critico => CRITICO")
    void critico() {
        assertThat(calc.status(99, 100)).isEqualTo(StatusEstoque.CRITICO);
        assertThat(calc.status(0, 100)).isEqualTo(StatusEstoque.CRITICO);
    }

    @Test
    @DisplayName("Entre o nivel critico e 25% acima => ATENCAO")
    void atencao() {
        assertThat(calc.status(100, 100)).isEqualTo(StatusEstoque.ATENCAO); // exatamente no minimo
        assertThat(calc.status(124, 100)).isEqualTo(StatusEstoque.ATENCAO); // < 125
    }

    @Test
    @DisplayName("A partir de 25% acima do nivel critico => OK")
    void ok() {
        assertThat(calc.status(125, 100)).isEqualTo(StatusEstoque.OK);
        assertThat(calc.status(500, 100)).isEqualTo(StatusEstoque.OK);
    }

    @Test
    @DisplayName("diasParaVencer conta os dias entre a referencia e a validade")
    void diasParaVencer() {
        LocalDate hoje = LocalDate.of(2026, 6, 9);
        assertThat(calc.diasParaVencer(LocalDate.of(2026, 6, 19), hoje)).isEqualTo(10);
        assertThat(calc.diasParaVencer(LocalDate.of(2026, 6, 9), hoje)).isEqualTo(0);
    }

    @Test
    @DisplayName("Validade no passado => dias negativos (ja vencido)")
    void vencido() {
        LocalDate hoje = LocalDate.of(2026, 6, 9);
        assertThat(calc.diasParaVencer(LocalDate.of(2026, 6, 4), hoje)).isEqualTo(-5);
    }
}
