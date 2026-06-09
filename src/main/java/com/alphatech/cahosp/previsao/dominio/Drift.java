package com.alphatech.cahosp.previsao.dominio;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

/**
 * Desvio (drift) do modelo de previsao — monitoramento continuo da degradacao. RF-PRV-06.
 * Rotulos pt-BR do front: Estavel/Atencao/Degradado.
 */
public enum Drift {
    ESTAVEL("Estável"),
    ATENCAO("Atenção"),
    DEGRADADO("Degradado");

    private final String rotulo;

    Drift(String rotulo) {
        this.rotulo = rotulo;
    }

    @JsonValue
    public String rotulo() {
        return rotulo;
    }

    @JsonCreator
    public static Drift fromJson(String valor) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("Drift e obrigatorio.");
        }
        String alvo = valor.trim();
        return Arrays.stream(values())
                .filter(d -> d.name().equalsIgnoreCase(alvo) || d.rotulo.equalsIgnoreCase(alvo))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Drift invalido: '" + valor + "'. Use Estavel, Atencao ou Degradado."));
    }
}
