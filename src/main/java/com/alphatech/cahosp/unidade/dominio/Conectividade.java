package com.alphatech.cahosp.unidade.dominio;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

/**
 * Conectividade da unidade com os sistemas centrais. RF-DAD-06 / RF-INT (modo offline-buffer
 * surge em unidades de conectividade "Precaria").
 */
public enum Conectividade {
    ESTAVEL("Estável"),
    INTERMITENTE("Intermitente"),
    PRECARIA("Precária");

    private final String rotulo;

    Conectividade(String rotulo) {
        this.rotulo = rotulo;
    }

    @JsonValue
    public String rotulo() {
        return rotulo;
    }

    @JsonCreator
    public static Conectividade fromJson(String valor) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("Conectividade e obrigatoria.");
        }
        String alvo = valor.trim();
        return Arrays.stream(values())
                .filter(c -> c.name().equalsIgnoreCase(alvo) || c.rotulo.equalsIgnoreCase(alvo))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Conectividade invalida: '" + valor + "'. Use Estavel, Intermitente ou Precaria."));
    }
}
