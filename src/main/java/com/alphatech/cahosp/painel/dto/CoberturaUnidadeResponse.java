package com.alphatech.cahosp.painel.dto;

import com.alphatech.cahosp.estoque.dominio.StatusEstoque;

/**
 * Ponto do grafico de cobertura por unidade (RF-DASH-01): sigla, percentual e status derivado.
 */
public record CoberturaUnidadeResponse(
        String nome,
        int valor,
        StatusEstoque status
) {
}
