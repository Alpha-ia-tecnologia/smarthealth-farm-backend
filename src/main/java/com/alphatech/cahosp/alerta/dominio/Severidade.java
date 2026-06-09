package com.alphatech.cahosp.alerta.dominio;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

/**
 * Severidade de um alerta (RF-ALE-01/02). Espelha o campo {@code severidade} do front
 * ({@code Crítico} / {@code Alto} / {@code Médio}), que dirige os tokens de status na UI
 * ({@code severidadeStatus} em src/lib/status.ts).
 */
public enum Severidade {
    CRITICO("Crítico"),
    ALTO("Alto"),
    MEDIO("Médio");

    private final String rotulo;

    Severidade(String rotulo) {
        this.rotulo = rotulo;
    }

    @JsonValue
    public String rotulo() {
        return rotulo;
    }

    @JsonCreator
    public static Severidade fromJson(String valor) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("Severidade e obrigatoria.");
        }
        String alvo = valor.trim();
        return Arrays.stream(values())
                .filter(s -> s.name().equalsIgnoreCase(alvo) || s.rotulo.equalsIgnoreCase(alvo))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Severidade invalida: '" + valor + "'. Use Crítico, Alto ou Médio."));
    }
}
