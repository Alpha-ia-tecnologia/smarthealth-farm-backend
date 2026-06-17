package com.alphatech.cahosp.ingestao.dominio;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

/**
 * Granularidade temporal da base historica por categoria (RF-DAD-04). Espelha o campo
 * {@code granularidade} do front ({@code Diária} / {@code Semanal} / {@code Mensal}).
 */
public enum GranularidadeDado {
    DIARIA("Diária"),
    SEMANAL("Semanal"),
    MENSAL("Mensal");

    private final String rotulo;

    GranularidadeDado(String rotulo) {
        this.rotulo = rotulo;
    }

    @JsonValue
    public String rotulo() {
        return rotulo;
    }

    @JsonCreator
    public static GranularidadeDado fromJson(String valor) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("Granularidade e obrigatoria.");
        }
        String alvo = valor.trim();
        return Arrays.stream(values())
                .filter(g -> g.name().equalsIgnoreCase(alvo) || g.rotulo.equalsIgnoreCase(alvo))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Granularidade invalida: '" + valor + "'. Use Diária, Semanal ou Mensal."));
    }
}
