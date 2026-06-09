package com.alphatech.cahosp.unidade.dto;

import jakarta.validation.constraints.NotNull;

/** Ativa ou desativa uma unidade. Nao ha exclusao fisica (LGPD/auditoria). RF-DAD-06. */
public record AlterarStatusUnidadeRequest(
        @NotNull(message = "O campo 'ativo' e obrigatorio.")
        Boolean ativo
) {
}
