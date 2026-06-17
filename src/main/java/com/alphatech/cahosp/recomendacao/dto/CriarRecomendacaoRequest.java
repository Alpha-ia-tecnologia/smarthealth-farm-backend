package com.alphatech.cahosp.recomendacao.dto;

import com.alphatech.cahosp.recomendacao.dominio.Prioridade;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

/**
 * Criação manual de uma transferência (redistribuição) entre unidades (RF-REC-05). Sempre tem
 * origem e destino; a economia é calculada no servidor a partir da quantidade. {@code prioridade}
 * e {@code justificativa} são opcionais (o serviço aplica padrões).
 */
public record CriarRecomendacaoRequest(
        @NotNull(message = "O insumo e obrigatorio.")
        UUID insumoId,
        @NotNull(message = "A unidade de origem e obrigatoria.")
        UUID unidadeOrigemId,
        @NotNull(message = "A unidade de destino e obrigatoria.")
        UUID unidadeDestinoId,
        @NotNull(message = "A quantidade e obrigatoria.")
        @Positive(message = "A quantidade deve ser maior que zero.")
        Integer quantidade,
        String justificativa,
        Prioridade prioridade
) {
}
