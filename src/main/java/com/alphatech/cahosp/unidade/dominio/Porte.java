package com.alphatech.cahosp.unidade.dominio;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

/**
 * Porte assistencial da unidade. RF-DAD-06.
 *
 * <p>Os rotulos do front sao "Pequeno"/"Medio"/"Grande"; aqui o nome da constante e o valor
 * persistido em maiusculo (sem acento). O JSON usa o rotulo pt-BR.
 */
public enum Porte {
    PEQUENO("Pequeno"),
    MEDIO("Médio"),
    GRANDE("Grande");

    private final String rotulo;

    Porte(String rotulo) {
        this.rotulo = rotulo;
    }

    @JsonValue
    public String rotulo() {
        return rotulo;
    }

    @JsonCreator
    public static Porte fromJson(String valor) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("Porte e obrigatorio.");
        }
        String alvo = valor.trim();
        return Arrays.stream(values())
                .filter(p -> p.name().equalsIgnoreCase(alvo) || p.rotulo.equalsIgnoreCase(alvo))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Porte invalido: '" + valor + "'. Use Pequeno, Medio ou Grande."));
    }
}
