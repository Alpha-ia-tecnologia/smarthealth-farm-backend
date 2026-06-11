package com.alphatech.cahosp.alerta;

import com.alphatech.cahosp.alerta.dominio.LimiarAlerta;
import com.alphatech.cahosp.alerta.dominio.Severidade;
import org.springframework.stereotype.Component;

/**
 * Regras puras de alerta (RF-ALE-01/02) — sem dependencia de banco, faceis de testar.
 * As bandas e limiares vem da configuracao {@link LimiarAlerta} (RF-ALE-03), entao alterar um
 * limiar muda de fato o disparo e a severidade calculados aqui.
 */
@Component
public class CalculadoraAlerta {

    /**
     * Cobertura em dias do saldo atual frente ao consumo medio diario (minimo de 1 dia).
     * Espelha {@code Math.max(1, Math.round(quantidade / consumoMedioDiario))} do front.
     */
    public long coberturaDias(int quantidade, int consumoMedioDiario) {
        if (consumoMedioDiario <= 0) {
            return Math.max(1, quantidade);
        }
        return Math.max(1, Math.round((double) quantidade / consumoMedioDiario));
    }

    /**
     * Gatilho do desabastecimento (RF-ALE-01/03): saldo abaixo de
     * {@code percentualEstoqueMinimo}% do estoque minimo. Com 100% equivale a
     * {@code quantidade < nivelCritico}.
     */
    public boolean abaixoDoEstoqueMinimo(int quantidade, int nivelCritico, int percentual) {
        return quantidade < nivelCritico * (percentual / 100.0);
    }

    /** Severidade do alerta de desabastecimento pela cobertura restante (RF-ALE-01). */
    public Severidade severidadePorCobertura(long diasCobertura, LimiarAlerta limiares) {
        if (diasCobertura <= limiares.getCoberturaCriticaDias()) {
            return Severidade.CRITICO;
        }
        if (diasCobertura <= limiares.getCoberturaAltaDias()) {
            return Severidade.ALTO;
        }
        return Severidade.MEDIO;
    }

    /** Severidade do alerta de vencimento pela antecedencia (RF-ALE-02). */
    public Severidade severidadePorVencimento(long diasParaVencer, LimiarAlerta limiares) {
        if (diasParaVencer <= limiares.getVencimentoCriticoDias()) {
            return Severidade.CRITICO;
        }
        if (diasParaVencer <= limiares.getVencimentoAltoDias()) {
            return Severidade.ALTO;
        }
        return Severidade.MEDIO;
    }

    /** Um lote com este prazo ja entra na janela de alerta de vencimento (RF-ALE-03)? */
    public boolean dentroDaJanelaVencimento(long diasParaVencer, LimiarAlerta limiares) {
        return diasParaVencer <= limiares.getAntecedenciaVencimentoDias();
    }
}
