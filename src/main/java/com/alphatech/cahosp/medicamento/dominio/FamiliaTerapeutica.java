package com.alphatech.cahosp.medicamento.dominio;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

/**
 * Familia terapeutica do medicamento. Mapeia 1:1 o tipo {@code FamiliaTerapeutica} do front
 * (src/types/index.ts). Filtro frequente em telas de previsao, estoque, alertas e relatorios —
 * por isso a coluna {@code familia} e indexada.
 */
public enum FamiliaTerapeutica {
    ANTIBIOTICOS("Antibióticos"),
    ANALGESICOS("Analgésicos"),
    ANTIVIRAIS("Antivirais"),
    CARDIOVASCULAR("Cardiovascular"),
    SOROS_E_VACINAS("Soros e Vacinas"),
    INSUMOS_MEDICOS("Insumos Médicos"),
    SAUDE_MENTAL("Saúde Mental"),
    ANTIPARASITARIOS("Antiparasitários");

    private final String rotulo;

    FamiliaTerapeutica(String rotulo) {
        this.rotulo = rotulo;
    }

    @JsonValue
    public String rotulo() {
        return rotulo;
    }

    @JsonCreator
    public static FamiliaTerapeutica fromJson(String valor) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("Familia terapeutica e obrigatoria.");
        }
        String alvo = valor.trim();
        return Arrays.stream(values())
                .filter(f -> f.name().equalsIgnoreCase(alvo) || f.rotulo.equalsIgnoreCase(alvo))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Familia terapeutica invalida: '" + valor + "'."));
    }
}
