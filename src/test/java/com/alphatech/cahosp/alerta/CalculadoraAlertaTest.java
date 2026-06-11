package com.alphatech.cahosp.alerta;

import com.alphatech.cahosp.alerta.dominio.LimiarAlerta;
import com.alphatech.cahosp.alerta.dominio.Severidade;
import com.alphatech.cahosp.comum.excecao.RegraNegocioException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testes unitarios das regras puras de alerta (RF-ALE-01/02/03): gatilho por percentual do
 * estoque minimo, bandas de severidade configuraveis, cobertura em dias e janela de vencimento.
 */
class CalculadoraAlertaTest {

    private final CalculadoraAlerta calc = new CalculadoraAlerta();

    /** Configuracao com os defaults da migration V12 (pct 100, bandas 5/10 e 20/40, janela 60). */
    private static LimiarAlerta limiaresPadrao() {
        return LimiarAlerta.criar(100, 5, 10, 60, 20, 40, true, true);
    }

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
    @DisplayName("Gatilho por percentual do estoque minimo: 100% = abaixo do nivel critico")
    void abaixoDoEstoqueMinimo() {
        assertThat(calc.abaixoDoEstoqueMinimo(199, 200, 100)).isTrue();
        assertThat(calc.abaixoDoEstoqueMinimo(200, 200, 100)).isFalse();
        // 150%: dispara mais cedo (saldo < 300)
        assertThat(calc.abaixoDoEstoqueMinimo(299, 200, 150)).isTrue();
        // 50%: dispara mais tarde (saldo < 100)
        assertThat(calc.abaixoDoEstoqueMinimo(150, 200, 50)).isFalse();
        assertThat(calc.abaixoDoEstoqueMinimo(99, 200, 50)).isTrue();
    }

    @Test
    @DisplayName("Severidade por cobertura usa as bandas configuradas (default <=5 Critico, <=10 Alto)")
    void severidadePorCobertura() {
        LimiarAlerta limiares = limiaresPadrao();
        assertThat(calc.severidadePorCobertura(1, limiares)).isEqualTo(Severidade.CRITICO);
        assertThat(calc.severidadePorCobertura(5, limiares)).isEqualTo(Severidade.CRITICO);
        assertThat(calc.severidadePorCobertura(6, limiares)).isEqualTo(Severidade.ALTO);
        assertThat(calc.severidadePorCobertura(10, limiares)).isEqualTo(Severidade.ALTO);
        assertThat(calc.severidadePorCobertura(11, limiares)).isEqualTo(Severidade.MEDIO);
    }

    @Test
    @DisplayName("Severidade por vencimento usa as bandas configuradas (default <=20 Critico, <=40 Alto)")
    void severidadePorVencimento() {
        LimiarAlerta limiares = limiaresPadrao();
        assertThat(calc.severidadePorVencimento(0, limiares)).isEqualTo(Severidade.CRITICO);
        assertThat(calc.severidadePorVencimento(20, limiares)).isEqualTo(Severidade.CRITICO);
        assertThat(calc.severidadePorVencimento(21, limiares)).isEqualTo(Severidade.ALTO);
        assertThat(calc.severidadePorVencimento(40, limiares)).isEqualTo(Severidade.ALTO);
        assertThat(calc.severidadePorVencimento(41, limiares)).isEqualTo(Severidade.MEDIO);
    }

    @Test
    @DisplayName("Bandas alteradas mudam a severidade calculada (limiar e vivo, nao constante)")
    void bandasConfiguraveis() {
        LimiarAlerta limiares = LimiarAlerta.criar(100, 2, 4, 90, 10, 30, true, true);
        assertThat(calc.severidadePorCobertura(3, limiares)).isEqualTo(Severidade.ALTO);
        assertThat(calc.severidadePorCobertura(5, limiares)).isEqualTo(Severidade.MEDIO);
        assertThat(calc.severidadePorVencimento(15, limiares)).isEqualTo(Severidade.ALTO);
        assertThat(calc.dentroDaJanelaVencimento(75, limiares)).isTrue();
    }

    @Test
    @DisplayName("Janela de vencimento configurada: dispara so dentro da antecedencia")
    void janelaVencimento() {
        LimiarAlerta limiares = limiaresPadrao();
        assertThat(calc.dentroDaJanelaVencimento(60, limiares)).isTrue();
        assertThat(calc.dentroDaJanelaVencimento(0, limiares)).isTrue();
        assertThat(calc.dentroDaJanelaVencimento(61, limiares)).isFalse();
    }

    @Test
    @DisplayName("Limiar invalido (banda alta <= critica; janela menor que a banda) => regra de negocio")
    void limiarInvalido() {
        assertThatThrownBy(() -> LimiarAlerta.criar(100, 10, 10, 60, 20, 40, true, true))
                .isInstanceOf(RegraNegocioException.class);
        assertThatThrownBy(() -> LimiarAlerta.criar(100, 5, 10, 60, 40, 40, true, true))
                .isInstanceOf(RegraNegocioException.class);
        assertThatThrownBy(() -> LimiarAlerta.criar(100, 5, 10, 30, 20, 40, true, true))
                .isInstanceOf(RegraNegocioException.class);
    }
}
