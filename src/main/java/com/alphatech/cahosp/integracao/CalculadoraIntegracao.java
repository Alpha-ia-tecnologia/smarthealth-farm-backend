package com.alphatech.cahosp.integracao;

import org.springframework.stereotype.Component;

import java.util.Collection;

/**
 * Regras puras de agregacao da integracao (RF-INT-02) — sem dependencia de banco, faceis de
 * testar. Espelha o KPI de latencia media do front (considera apenas conexoes ativas, latencia &gt; 0).
 */
@Component
public class CalculadoraIntegracao {

    /**
     * Latencia media (ms) considerando apenas integracoes com latencia positiva — uma integracao
     * indisponivel (latencia 0) nao deve baixar artificialmente a media. RF-INT-02.
     */
    public int latenciaMediaMs(Collection<Integer> latencias) {
        if (latencias == null) {
            return 0;
        }
        int[] ativas = latencias.stream().mapToInt(Integer::intValue).filter(l -> l > 0).toArray();
        if (ativas.length == 0) {
            return 0;
        }
        long soma = 0;
        for (int latencia : ativas) {
            soma += latencia;
        }
        return Math.round((float) soma / ativas.length);
    }
}
