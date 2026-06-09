package com.alphatech.cahosp.medicamento.dominio;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

/**
 * Criticidade clinico-operacional do medicamento. Alimenta o calculo de severidade de alertas
 * (RF-ALE) e a priorizacao de recomendacoes de reposicao (RF-REC).
 */
public enum Criticidade {
    ALTA("Alta"),
    MEDIA("Média"),
    BAIXA("Baixa");

    private final String rotulo;

    Criticidade(String rotulo) {
        this.rotulo = rotulo;
    }

    @JsonValue
    public String rotulo() {
        return rotulo;
    }

    @JsonCreator
    public static Criticidade fromJson(String valor) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("Criticidade e obrigatoria.");
        }
        String alvo = valor.trim();
        return Arrays.stream(values())
                .filter(c -> c.name().equalsIgnoreCase(alvo) || c.rotulo.equalsIgnoreCase(alvo))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Criticidade invalida: '" + valor + "'. Use Alta, Media ou Baixa."));
    }
}
