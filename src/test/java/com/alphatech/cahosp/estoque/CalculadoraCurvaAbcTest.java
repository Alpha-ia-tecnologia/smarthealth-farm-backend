package com.alphatech.cahosp.estoque;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CalculadoraCurvaAbcTest {

    private final CalculadoraCurvaAbc calc = new CalculadoraCurvaAbc();

    private static CalculadoraCurvaAbc.Entrada e(String valor) {
        return new CalculadoraCurvaAbc.Entrada(UUID.randomUUID(), new BigDecimal(valor));
    }

    @Test
    @DisplayName("Classifica A/B/C pela participacao acumulada (80/95) e ordena por valor desc")
    void classifica() {
        List<CalculadoraCurvaAbc.Item> itens = calc.classificar(List.of(e("50"), e("800"), e("150")));

        assertThat(itens).extracting(i -> i.classe().name()).containsExactly("A", "B", "C");
        assertThat(itens).extracting(CalculadoraCurvaAbc.Item::valorConsumo)
                .containsExactly(new BigDecimal("800"), new BigDecimal("150"), new BigDecimal("50"));
        assertThat(itens.get(0).participacaoPct()).isEqualByComparingTo("80.00");
        assertThat(itens.get(0).acumuladoPct()).isEqualByComparingTo("80.00");
        assertThat(itens.get(1).acumuladoPct()).isEqualByComparingTo("95.00");
        assertThat(itens.get(2).acumuladoPct()).isEqualByComparingTo("100.00");
    }

    @Test
    @DisplayName("O item que cruza a faixa de 80% ainda entra na classe A")
    void itemQueCruza80EhA() {
        // Ordenado: 78, 12, 10. O segundo cruza 80 (78 -> 90), mas o acumulado ANTES dele (78) < 80.
        List<CalculadoraCurvaAbc.Item> itens = calc.classificar(List.of(e("78"), e("10"), e("12")));
        assertThat(itens.get(0).classe().name()).isEqualTo("A");
        assertThat(itens.get(1).classe().name()).isEqualTo("A");
        assertThat(itens.get(2).classe().name()).isEqualTo("B");
    }

    @Test
    @DisplayName("Total zero nao quebra e classifica tudo como C")
    void totalZero() {
        List<CalculadoraCurvaAbc.Item> itens = calc.classificar(List.of(e("0"), e("0")));
        assertThat(itens).extracting(i -> i.classe().name()).containsOnly("C");
        assertThat(itens.get(0).participacaoPct()).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("Lista vazia retorna vazio")
    void vazio() {
        assertThat(calc.classificar(List.of())).isEmpty();
    }
}
