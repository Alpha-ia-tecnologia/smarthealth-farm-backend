package com.alphatech.cahosp.alerta;

import com.alphatech.cahosp.alerta.dominio.Severidade;
import org.springframework.stereotype.Component;

/**
 * Regras puras de alerta (RF-ALE-01/02) — sem dependencia de banco, faceis de testar.
 * Espelha os limiares e bandas de severidade do front (src/data/index.ts e AlertasPage.tsx):
 *
 * <ul>
 *   <li><strong>Desabastecimento:</strong> cobertura = saldo / consumo medio diario; severidade
 *       {@code Crítico} ate 5 dias, {@code Alto} ate 10, senao {@code Médio}.</li>
 *   <li><strong>Vencimento:</strong> dispara quando faltam ate 60 dias para a validade; severidade
 *       {@code Crítico} ate 20 dias, {@code Alto} ate 40, senao {@code Médio}.</li>
 * </ul>
 */
@Component
public class CalculadoraAlerta {

    /** Antecedencia (dias) para um lote disparar alerta de vencimento (RF-ALE-03). */
    public static final long JANELA_VENCIMENTO_DIAS = 60;

    private static final long COBERTURA_CRITICA_DIAS = 5;
    private static final long COBERTURA_ALTA_DIAS = 10;
    private static final long VENCIMENTO_CRITICO_DIAS = 20;
    private static final long VENCIMENTO_ALTO_DIAS = 40;

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

    /** Severidade do alerta de desabastecimento pela cobertura restante (RF-ALE-01). */
    public Severidade severidadePorCobertura(long diasCobertura) {
        if (diasCobertura <= COBERTURA_CRITICA_DIAS) {
            return Severidade.CRITICO;
        }
        if (diasCobertura <= COBERTURA_ALTA_DIAS) {
            return Severidade.ALTO;
        }
        return Severidade.MEDIO;
    }

    /** Severidade do alerta de vencimento pela antecedencia (RF-ALE-02). */
    public Severidade severidadePorVencimento(long diasParaVencer) {
        if (diasParaVencer <= VENCIMENTO_CRITICO_DIAS) {
            return Severidade.CRITICO;
        }
        if (diasParaVencer <= VENCIMENTO_ALTO_DIAS) {
            return Severidade.ALTO;
        }
        return Severidade.MEDIO;
    }

    /** Um lote com este prazo de validade ja entra na janela de alerta de vencimento? */
    public boolean dentroDaJanelaVencimento(long diasParaVencer) {
        return diasParaVencer <= JANELA_VENCIMENTO_DIAS;
    }
}
