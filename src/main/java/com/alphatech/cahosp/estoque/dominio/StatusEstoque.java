package com.alphatech.cahosp.estoque.dominio;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

/**
 * Situacao do estoque de uma posicao (RF-EST-04). Derivado (nao persistido): calculado a partir
 * da quantidade frente ao nivel critico. Os rotulos casam com os tokens de status do front
 * ({@code ok}/{@code atencao}/{@code critico}) consumidos pelos badges/graficos.
 */
public enum StatusEstoque {
    OK("ok"),
    ATENCAO("atencao"),
    CRITICO("critico");

    private final String rotulo;

    StatusEstoque(String rotulo) {
        this.rotulo = rotulo;
    }

    @JsonValue
    public String rotulo() {
        return rotulo;
    }

    @JsonCreator
    public static StatusEstoque fromJson(String valor) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("Status de estoque e obrigatorio.");
        }
        String alvo = valor.trim();
        return Arrays.stream(values())
                .filter(s -> s.name().equalsIgnoreCase(alvo) || s.rotulo.equalsIgnoreCase(alvo))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Status de estoque invalido: '" + valor + "'. Use ok, atencao ou critico."));
    }
}
