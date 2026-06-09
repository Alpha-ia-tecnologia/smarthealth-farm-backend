package com.alphatech.cahosp.alerta.dominio;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

/**
 * Ciclo de tratamento de um alerta (RF-ALE-05). Espelha o campo {@code status} do front
 * ({@code Aberto} / {@code Em tratamento} / {@code Resolvido}).
 *
 * <p>Transicoes validas: {@code ABERTO -> EM_TRATAMENTO -> RESOLVIDO}, com possibilidade de
 * resolver direto a partir de aberto e de reabrir um alerta em tratamento. {@code RESOLVIDO} e
 * terminal (a regeneracao do motor cria um novo alerta se a condicao voltar a ocorrer).
 */
public enum StatusAlerta {
    ABERTO("Aberto"),
    EM_TRATAMENTO("Em tratamento"),
    RESOLVIDO("Resolvido");

    private final String rotulo;

    StatusAlerta(String rotulo) {
        this.rotulo = rotulo;
    }

    @JsonValue
    public String rotulo() {
        return rotulo;
    }

    @JsonCreator
    public static StatusAlerta fromJson(String valor) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("Status do alerta e obrigatorio.");
        }
        String alvo = valor.trim();
        return Arrays.stream(values())
                .filter(s -> s.name().equalsIgnoreCase(alvo) || s.rotulo.equalsIgnoreCase(alvo))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Status invalido: '" + valor + "'. Use Aberto, Em tratamento ou Resolvido."));
    }
}
