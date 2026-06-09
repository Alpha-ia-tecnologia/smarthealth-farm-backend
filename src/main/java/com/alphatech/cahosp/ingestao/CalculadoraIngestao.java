package com.alphatech.cahosp.ingestao;

import org.springframework.stereotype.Component;

import java.util.Collection;

/**
 * Regras puras de agregacao da ingestao (RF-DAD-04) — sem dependencia de banco, faceis de
 * testar. Espelha os KPIs calculados no front (qualidade media arredondada).
 */
@Component
public class CalculadoraIngestao {

    /** Qualidade media (0-100) arredondada sobre as fontes informadas. RF-DAD-04. */
    public int qualidadeMedia(Collection<Integer> qualidades) {
        if (qualidades == null || qualidades.isEmpty()) {
            return 0;
        }
        int soma = qualidades.stream().mapToInt(Integer::intValue).sum();
        return Math.round((float) soma / qualidades.size());
    }
}
