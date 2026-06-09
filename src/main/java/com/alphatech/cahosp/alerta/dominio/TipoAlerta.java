package com.alphatech.cahosp.alerta.dominio;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

/**
 * Tipo de alerta operacional (RF-ALE-01/02). Espelha o tipo {@code TipoAlerta} do front
 * (src/types/index.ts): {@code Desabastecimento} (cobertura abaixo do nivel critico) e
 * {@code Vencimento} (lote proximo da validade).
 *
 * <p>O nome da constante e o valor persistido (maiusculo); o rotulo pt-BR e o valor serializado
 * em JSON e aceito tambem como filtro (ex.: {@code ?tipo=Desabastecimento}).
 */
public enum TipoAlerta {
    DESABASTECIMENTO("Desabastecimento"),
    VENCIMENTO("Vencimento");

    private final String rotulo;

    TipoAlerta(String rotulo) {
        this.rotulo = rotulo;
    }

    @JsonValue
    public String rotulo() {
        return rotulo;
    }

    @JsonCreator
    public static TipoAlerta fromJson(String valor) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("Tipo de alerta e obrigatorio.");
        }
        String alvo = valor.trim();
        return Arrays.stream(values())
                .filter(t -> t.name().equalsIgnoreCase(alvo) || t.rotulo.equalsIgnoreCase(alvo))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Tipo de alerta invalido: '" + valor + "'. Use Desabastecimento ou Vencimento."));
    }
}
