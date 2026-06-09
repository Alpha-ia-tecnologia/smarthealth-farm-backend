package com.alphatech.cahosp.estoque.dominio;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

/**
 * Tipo de movimentacao no livro-razao do estoque (RF-EST). Mapeia 1:1 o tipo do front.
 *
 * <p>Efeito no saldo do lote: {@code ENTRADA} soma; {@code SAIDA} e {@code TRANSFERENCIA}
 * subtraem; {@code AJUSTE} corrige o saldo para o valor contado (recontagem de inventario).
 */
public enum TipoMovimentacao {
    ENTRADA("Entrada"),
    SAIDA("Saída"),
    TRANSFERENCIA("Transferência"),
    AJUSTE("Ajuste");

    private final String rotulo;

    TipoMovimentacao(String rotulo) {
        this.rotulo = rotulo;
    }

    @JsonValue
    public String rotulo() {
        return rotulo;
    }

    @JsonCreator
    public static TipoMovimentacao fromJson(String valor) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("Tipo de movimentacao e obrigatorio.");
        }
        String alvo = valor.trim();
        return Arrays.stream(values())
                .filter(t -> t.name().equalsIgnoreCase(alvo) || t.rotulo.equalsIgnoreCase(alvo))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Tipo de movimentacao invalido: '" + valor + "'. Use Entrada, Saida, Transferencia ou Ajuste."));
    }
}
