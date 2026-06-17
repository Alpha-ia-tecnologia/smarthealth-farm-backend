package com.alphatech.cahosp.insumo.dto;

import jakarta.validation.constraints.NotNull;

/** Ativa ou desativa um insumo. Sem exclusao fisica (LGPD/auditoria). RF-DAD-06. */
public record AlterarStatusInsumoRequest(
        @NotNull(message = "O campo 'ativo' e obrigatorio.")
        Boolean ativo
) {
}
