package com.alphatech.cahosp.alerta.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Atualizacao dos limiares de disparo dos alertas (RF-ALE-03 — acao de Gestor). As faixas simples
 * sao validadas aqui; a coerencia entre bandas (alto > critico, janela >= banda) e regra de
 * negocio da entidade (422).
 */
public record AtualizarLimiarAlertaRequest(
        @NotNull(message = "O percentual do estoque mínimo é obrigatório.")
        @Min(value = 10, message = "O percentual do estoque mínimo deve ser de no mínimo 10%.")
        @Max(value = 200, message = "O percentual do estoque mínimo deve ser de no máximo 200%.")
        Integer percentualEstoqueMinimo,

        @NotNull(message = "A banda crítica de cobertura é obrigatória.")
        @Min(value = 1, message = "A banda crítica de cobertura deve ser de pelo menos 1 dia.")
        @Max(value = 365, message = "A banda crítica de cobertura deve caber em 365 dias.")
        Integer coberturaCriticaDias,

        @NotNull(message = "A banda alta de cobertura é obrigatória.")
        @Min(value = 1, message = "A banda alta de cobertura deve ser de pelo menos 1 dia.")
        @Max(value = 365, message = "A banda alta de cobertura deve caber em 365 dias.")
        Integer coberturaAltaDias,

        @NotNull(message = "A antecedência de vencimento é obrigatória.")
        @Min(value = 1, message = "A antecedência de vencimento deve ser de pelo menos 1 dia.")
        @Max(value = 365, message = "A antecedência de vencimento deve caber em 365 dias.")
        Integer antecedenciaVencimentoDias,

        @NotNull(message = "A banda crítica de vencimento é obrigatória.")
        @Min(value = 1, message = "A banda crítica de vencimento deve ser de pelo menos 1 dia.")
        @Max(value = 365, message = "A banda crítica de vencimento deve caber em 365 dias.")
        Integer vencimentoCriticoDias,

        @NotNull(message = "A banda alta de vencimento é obrigatória.")
        @Min(value = 1, message = "A banda alta de vencimento deve ser de pelo menos 1 dia.")
        @Max(value = 365, message = "A banda alta de vencimento deve caber em 365 dias.")
        Integer vencimentoAltoDias,

        @NotNull(message = "Informe se o alerta de desabastecimento está ativo.")
        Boolean desabastecimentoAtivo,

        @NotNull(message = "Informe se o alerta de vencimento está ativo.")
        Boolean vencimentoAtivo
) {
}
