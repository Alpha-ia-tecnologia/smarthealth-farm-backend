package com.alphatech.cahosp.recomendacao.dominio;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

/**
 * Tipo de recomendacao (RF-REC-01). Espelha o tipo {@code TipoRecomendacao} do front:
 * <ul>
 *   <li>{@code REPOSICAO} — compra/reposicao dimensionada pela previsao para restaurar o nivel
 *       de seguranca de uma unidade;</li>
 *   <li>{@code REDISTRIBUICAO} — transferencia de excedente de uma unidade para outra em risco,
 *       evitando compra emergencial.</li>
 * </ul>
 */
public enum TipoRecomendacao {
    REPOSICAO("Reposição"),
    REDISTRIBUICAO("Redistribuição");

    private final String rotulo;

    TipoRecomendacao(String rotulo) {
        this.rotulo = rotulo;
    }

    @JsonValue
    public String rotulo() {
        return rotulo;
    }

    @JsonCreator
    public static TipoRecomendacao fromJson(String valor) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("Tipo de recomendacao e obrigatorio.");
        }
        String alvo = valor.trim();
        return Arrays.stream(values())
                .filter(t -> t.name().equalsIgnoreCase(alvo) || t.rotulo.equalsIgnoreCase(alvo))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Tipo de recomendacao invalido: '" + valor + "'. Use Reposição ou Redistribuição."));
    }
}
