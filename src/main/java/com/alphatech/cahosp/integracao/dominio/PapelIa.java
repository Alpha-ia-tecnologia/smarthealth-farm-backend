package com.alphatech.cahosp.integracao.dominio;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

/**
 * Papel de um provedor de IA no AI Gateway (RF-INT-06). Espelha o campo {@code papel} do front:
 * {@code Primário} (usado por padrao), {@code Fallback} (substituto) e {@code Standby} (reserva).
 */
public enum PapelIa {
    PRIMARIO("Primário"),
    FALLBACK("Fallback"),
    STANDBY("Standby");

    private final String rotulo;

    PapelIa(String rotulo) {
        this.rotulo = rotulo;
    }

    @JsonValue
    public String rotulo() {
        return rotulo;
    }

    @JsonCreator
    public static PapelIa fromJson(String valor) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("Papel do provedor e obrigatorio.");
        }
        String alvo = valor.trim();
        return Arrays.stream(values())
                .filter(p -> p.name().equalsIgnoreCase(alvo) || p.rotulo.equalsIgnoreCase(alvo))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Papel invalido: '" + valor + "'. Use Primário, Fallback ou Standby."));
    }
}
