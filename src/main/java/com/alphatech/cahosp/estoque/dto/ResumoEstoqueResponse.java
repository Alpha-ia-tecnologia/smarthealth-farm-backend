package com.alphatech.cahosp.estoque.dto;

/**
 * KPIs da tela de estoque (RF-EST-01/04/05): itens abaixo do minimo, lotes proximos do
 * vencimento (<= 60 dias), tempo medio de ressuprimento e total de unidades em estoque.
 */
public record ResumoEstoqueResponse(
        long itensCriticos,
        long lotesProximosVencimento,
        long tempoMedioRessuprimentoDias,
        long totalUnidadesEstoque
) {
}
