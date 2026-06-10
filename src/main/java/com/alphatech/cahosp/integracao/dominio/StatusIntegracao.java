package com.alphatech.cahosp.integracao.dominio;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

/**
 * Situacao de uma integracao com sistema externo da EMSERH (RF-INT-01/02). Espelha o campo
 * {@code status} do front ({@code Operacional} / {@code Degradada} / {@code Indisponível}).
 */
public enum StatusIntegracao {
    OPERACIONAL("Operacional"),
    DEGRADADA("Degradada"),
    INDISPONIVEL("Indisponível");

    private final String rotulo;

    StatusIntegracao(String rotulo) {
        this.rotulo = rotulo;
    }

    @JsonValue
    public String rotulo() {
        return rotulo;
    }

    @JsonCreator
    public static StatusIntegracao fromJson(String valor) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("Status da integracao e obrigatorio.");
        }
        String alvo = valor.trim();
        return Arrays.stream(values())
                .filter(s -> s.name().equalsIgnoreCase(alvo) || s.rotulo.equalsIgnoreCase(alvo))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Status invalido: '" + valor + "'. Use Operacional, Degradada ou Indisponível."));
    }
}
