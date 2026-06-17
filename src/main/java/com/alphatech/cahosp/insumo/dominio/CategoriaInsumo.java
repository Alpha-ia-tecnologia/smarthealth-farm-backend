package com.alphatech.cahosp.insumo.dominio;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

/**
 * Categoria do insumo. Mapeia 1:1 o tipo {@code CategoriaInsumo} do front
 * (src/types/index.ts). Filtro frequente em telas de previsao, estoque, alertas e relatorios —
 * por isso a coluna {@code categoria} e indexada.
 */
public enum CategoriaInsumo {
    ANTIBIOTICOS("Antibióticos"),
    ANALGESICOS("Analgésicos"),
    ANTIVIRAIS("Antivirais"),
    CARDIOVASCULAR("Cardiovascular"),
    SOROS_E_VACINAS("Soros e Vacinas"),
    INSUMOS_MEDICOS("Insumos Médicos"),
    SAUDE_MENTAL("Saúde Mental"),
    ANTIPARASITARIOS("Antiparasitários");

    private final String rotulo;

    CategoriaInsumo(String rotulo) {
        this.rotulo = rotulo;
    }

    @JsonValue
    public String rotulo() {
        return rotulo;
    }

    @JsonCreator
    public static CategoriaInsumo fromJson(String valor) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("Categoria e obrigatoria.");
        }
        String alvo = valor.trim();
        return Arrays.stream(values())
                .filter(f -> f.name().equalsIgnoreCase(alvo) || f.rotulo.equalsIgnoreCase(alvo))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Categoria invalida: '" + valor + "'."));
    }
}
