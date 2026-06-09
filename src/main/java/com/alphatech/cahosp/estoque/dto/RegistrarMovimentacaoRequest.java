package com.alphatech.cahosp.estoque.dto;

import com.alphatech.cahosp.estoque.dominio.TipoMovimentacao;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.UUID;

/**
 * Registra um lancamento no livro-razao sobre um lote (RF-EST-06). Para {@code AJUSTE}, a
 * quantidade e o saldo contado (pode ser 0); para os demais tipos, deve ser positiva (validado
 * no servico).
 */
public record RegistrarMovimentacaoRequest(
        @NotNull(message = "O lote e obrigatorio.")
        UUID loteId,

        @NotNull(message = "O tipo de movimentacao e obrigatorio.")
        TipoMovimentacao tipo,

        @PositiveOrZero(message = "A quantidade nao pode ser negativa.")
        int quantidade,

        @NotBlank(message = "O responsavel e obrigatorio.")
        String responsavel,

        @NotBlank(message = "O documento e obrigatorio.")
        String documento
) {
}
