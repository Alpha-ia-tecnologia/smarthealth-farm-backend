package com.alphatech.cahosp.estoque.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Entrada de um novo lote (RF-EST-03). A quantidade inicial gera uma movimentacao de Entrada
 * no livro-razao.
 */
public record CriarLoteRequest(
        @NotNull(message = "O insumo e obrigatorio.")
        UUID insumoId,

        @NotNull(message = "A unidade e obrigatoria.")
        UUID unidadeId,

        @NotBlank(message = "O numero do lote e obrigatorio.")
        String numeroLote,

        @NotNull(message = "A validade e obrigatoria.")
        @Future(message = "A validade deve ser uma data futura.")
        LocalDate validade,

        @Positive(message = "A quantidade inicial deve ser positiva.")
        int quantidade,

        @NotBlank(message = "O fabricante e obrigatorio.")
        String fabricante,

        @NotBlank(message = "O responsavel e obrigatorio.")
        String responsavel,

        @NotBlank(message = "O documento e obrigatorio.")
        String documento
) {
}
