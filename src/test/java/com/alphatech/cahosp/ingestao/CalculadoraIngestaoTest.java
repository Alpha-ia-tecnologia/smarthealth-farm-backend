package com.alphatech.cahosp.ingestao;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes unitarios das regras puras de ingestao (sem Spring, sem Docker). RF-DAD-04.
 */
class CalculadoraIngestaoTest {

    private final CalculadoraIngestao calc = new CalculadoraIngestao();

    @Test
    @DisplayName("qualidadeMedia retorna 0 quando nao ha fontes")
    void vazio() {
        assertThat(calc.qualidadeMedia(List.of())).isZero();
    }

    @Test
    @DisplayName("qualidadeMedia arredonda a media das fontes")
    void media() {
        assertThat(calc.qualidadeMedia(List.of(82, 91, 64, 77, 70, 96))).isEqualTo(80);
        assertThat(calc.qualidadeMedia(List.of(80, 85))).isEqualTo(83);
    }
}
