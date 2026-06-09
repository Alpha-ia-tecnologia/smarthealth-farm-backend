package com.alphatech.cahosp.painel.dto;

import com.alphatech.cahosp.estoque.dominio.StatusEstoque;
import com.alphatech.cahosp.unidade.dominio.Conectividade;

import java.util.UUID;

/**
 * Resumo operacional de uma unidade atendida (RF-DASH-02), espelhando {@code resumoUnidade} do
 * front: cobertura, itens criticos/em atencao, alertas ativos e status agregado.
 */
public record ResumoUnidadeResponse(
        UUID unidadeId,
        String sigla,
        String nome,
        String municipio,
        Conectividade conectividade,
        int itens,
        int criticos,
        int atencao,
        int alertasAtivos,
        int cobertura,
        StatusEstoque statusCobertura,
        StatusEstoque statusUnidade
) {
}
