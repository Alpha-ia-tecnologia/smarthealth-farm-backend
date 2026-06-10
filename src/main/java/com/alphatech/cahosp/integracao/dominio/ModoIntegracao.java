package com.alphatech.cahosp.integracao.dominio;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

/**
 * Modo de operacao de uma integracao para unidades de borda com conectividade precaria
 * (RF-INT-04/05). Espelha o campo {@code modo} do front: {@code Online}, {@code Offline (buffer)}
 * (acumula localmente) e {@code Reconciliando} (sincronizando o buffer ao voltar a conexao).
 */
public enum ModoIntegracao {
    ONLINE("Online"),
    OFFLINE_BUFFER("Offline (buffer)"),
    RECONCILIANDO("Reconciliando");

    private final String rotulo;

    ModoIntegracao(String rotulo) {
        this.rotulo = rotulo;
    }

    @JsonValue
    public String rotulo() {
        return rotulo;
    }

    @JsonCreator
    public static ModoIntegracao fromJson(String valor) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("Modo da integracao e obrigatorio.");
        }
        String alvo = valor.trim();
        return Arrays.stream(values())
                .filter(m -> m.name().equalsIgnoreCase(alvo) || m.rotulo.equalsIgnoreCase(alvo))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Modo invalido: '" + valor + "'. Use Online, Offline (buffer) ou Reconciliando."));
    }
}
