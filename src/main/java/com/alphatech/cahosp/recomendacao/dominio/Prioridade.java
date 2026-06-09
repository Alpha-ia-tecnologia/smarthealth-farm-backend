package com.alphatech.cahosp.recomendacao.dominio;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

/**
 * Prioridade de uma recomendacao (RF-REC). Espelha o tipo {@code Prioridade} do front
 * ({@code Essencial} / {@code Importante} / {@code Desejável}), usado para ordenar e destacar
 * recomendacoes na fila de aprovacao.
 */
public enum Prioridade {
    ESSENCIAL("Essencial"),
    IMPORTANTE("Importante"),
    DESEJAVEL("Desejável");

    private final String rotulo;

    Prioridade(String rotulo) {
        this.rotulo = rotulo;
    }

    @JsonValue
    public String rotulo() {
        return rotulo;
    }

    @JsonCreator
    public static Prioridade fromJson(String valor) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("Prioridade e obrigatoria.");
        }
        String alvo = valor.trim();
        return Arrays.stream(values())
                .filter(p -> p.name().equalsIgnoreCase(alvo) || p.rotulo.equalsIgnoreCase(alvo))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Prioridade invalida: '" + valor + "'. Use Essencial, Importante ou Desejável."));
    }
}
