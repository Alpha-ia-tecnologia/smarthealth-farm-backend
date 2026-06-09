package com.alphatech.cahosp.ingestao.dominio;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

/**
 * Situacao de sincronizacao de uma fonte de dados (RF-DAD-02). Espelha o campo {@code status}
 * do front ({@code Sincronizado} / {@code Atrasado} / {@code Erro}).
 */
public enum StatusFonte {
    SINCRONIZADO("Sincronizado"),
    ATRASADO("Atrasado"),
    ERRO("Erro");

    private final String rotulo;

    StatusFonte(String rotulo) {
        this.rotulo = rotulo;
    }

    @JsonValue
    public String rotulo() {
        return rotulo;
    }

    @JsonCreator
    public static StatusFonte fromJson(String valor) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("Status da fonte e obrigatorio.");
        }
        String alvo = valor.trim();
        return Arrays.stream(values())
                .filter(s -> s.name().equalsIgnoreCase(alvo) || s.rotulo.equalsIgnoreCase(alvo))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Status invalido: '" + valor + "'. Use Sincronizado, Atrasado ou Erro."));
    }
}
