package com.alphatech.cahosp.recomendacao;

import com.alphatech.cahosp.medicamento.dominio.Criticidade;
import com.alphatech.cahosp.recomendacao.dominio.Prioridade;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes unitarios do dimensionamento de recomendacoes (RF-REC): reposicao, redistribuicao,
 * excedente e prioridade.
 */
class CalculadoraRecomendacaoTest {

    private final CalculadoraRecomendacao calc = new CalculadoraRecomendacao();

    @Test
    @DisplayName("Reposicao = max(20, estoqueMaximo - quantidade)")
    void quantidadeReposicao() {
        assertThat(calc.quantidadeReposicao(1000, 300)).isEqualTo(700);
        assertThat(calc.quantidadeReposicao(1000, 995)).isEqualTo(20);   // piso
        assertThat(calc.quantidadeReposicao(1000, 1200)).isEqualTo(20);  // ja acima do maximo -> piso
    }

    @Test
    @DisplayName("Redistribuicao leva o destino a 1,5x o nivel critico, com piso de 10")
    void quantidadeRedistribuicao() {
        // 1,5 * 200 - 50 = 250
        assertThat(calc.quantidadeRedistribuicao(200, 50)).isEqualTo(250);
        // 1,5 * 100 - 200 = -50 -> usa o nivel critico (100)
        assertThat(calc.quantidadeRedistribuicao(100, 200)).isEqualTo(100);
        // 1,5 * 10 - 12 = 3 -> piso 10
        assertThat(calc.quantidadeRedistribuicao(10, 12)).isEqualTo(10);
    }

    @Test
    @DisplayName("Excedente = saldo acima do dobro do nivel critico")
    void ehExcedente() {
        assertThat(calc.ehExcedente(201, 100)).isTrue();
        assertThat(calc.ehExcedente(200, 100)).isFalse();
        assertThat(calc.ehExcedente(50, 100)).isFalse();
    }

    @Test
    @DisplayName("Prioridade: criticidade Alta => Essencial; demais => Importante")
    void prioridadePorCriticidade() {
        assertThat(calc.prioridadePorCriticidade(Criticidade.ALTA)).isEqualTo(Prioridade.ESSENCIAL);
        assertThat(calc.prioridadePorCriticidade(Criticidade.MEDIA)).isEqualTo(Prioridade.IMPORTANTE);
        assertThat(calc.prioridadePorCriticidade(Criticidade.BAIXA)).isEqualTo(Prioridade.IMPORTANTE);
    }
}
