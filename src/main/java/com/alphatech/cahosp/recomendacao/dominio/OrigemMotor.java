package com.alphatech.cahosp.recomendacao.dominio;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

/**
 * Origem do motor que gerou a recomendacao (RF-REC-03): {@code REGRAS} (heuristica deterministica)
 * ou {@code APRENDIZADO_MAQUINA} (assistida por IA). Espelha {@code origemMotor} do front e
 * sustenta a evolucao "regras -> IA" exibida no painel de desempenho.
 */
public enum OrigemMotor {
    REGRAS("Regras"),
    APRENDIZADO_MAQUINA("Aprendizado de Máquina");

    private final String rotulo;

    OrigemMotor(String rotulo) {
        this.rotulo = rotulo;
    }

    @JsonValue
    public String rotulo() {
        return rotulo;
    }

    @JsonCreator
    public static OrigemMotor fromJson(String valor) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("Origem do motor e obrigatoria.");
        }
        String alvo = valor.trim();
        return Arrays.stream(values())
                .filter(o -> o.name().equalsIgnoreCase(alvo) || o.rotulo.equalsIgnoreCase(alvo))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Origem do motor invalida: '" + valor + "'. Use Regras ou Aprendizado de Máquina."));
    }
}
