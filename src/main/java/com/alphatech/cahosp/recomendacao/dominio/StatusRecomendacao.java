package com.alphatech.cahosp.recomendacao.dominio;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

/**
 * Ciclo de vida de uma recomendacao (RF-REC-05). Espelha o campo {@code status} do front
 * ({@code Pendente} / {@code Aprovada} / {@code Executada}).
 *
 * <p>Fluxo: {@code PENDENTE -> APROVADA -> EXECUTADA}. Aprovar e executar sao acoes de Gestor;
 * a regeneracao do motor renova apenas as pendentes (preserva aprovadas/executadas).
 */
public enum StatusRecomendacao {
    PENDENTE("Pendente"),
    APROVADA("Aprovada"),
    EXECUTADA("Executada");

    private final String rotulo;

    StatusRecomendacao(String rotulo) {
        this.rotulo = rotulo;
    }

    @JsonValue
    public String rotulo() {
        return rotulo;
    }

    @JsonCreator
    public static StatusRecomendacao fromJson(String valor) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("Status da recomendacao e obrigatorio.");
        }
        String alvo = valor.trim();
        return Arrays.stream(values())
                .filter(s -> s.name().equalsIgnoreCase(alvo) || s.rotulo.equalsIgnoreCase(alvo))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Status invalido: '" + valor + "'. Use Pendente, Aprovada ou Executada."));
    }
}
