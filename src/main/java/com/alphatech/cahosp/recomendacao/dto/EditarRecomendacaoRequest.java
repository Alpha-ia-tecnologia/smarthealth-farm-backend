package com.alphatech.cahosp.recomendacao.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

/**
 * Edição de uma recomendação ainda pendente (RF-REC-05): ajusta medicamento, unidades e quantidade.
 * {@code unidadeOrigemId} é obrigatório quando a recomendação é uma redistribuição e nulo numa
 * reposição (o serviço valida conforme o tipo). A economia é recalculada no servidor.
 */
public record EditarRecomendacaoRequest(
        @NotNull(message = "O medicamento e obrigatorio.")
        UUID medicamentoId,
        UUID unidadeOrigemId,
        @NotNull(message = "A unidade de destino e obrigatoria.")
        UUID unidadeDestinoId,
        @NotNull(message = "A quantidade e obrigatoria.")
        @Positive(message = "A quantidade deve ser maior que zero.")
        Integer quantidade
) {
}
