package com.alphatech.cahosp.usuario.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Ativa ou desativa um usuario (RF-ADM-01). Nao ha exclusao fisica (LGPD/auditoria) —
 * o desligamento e feito por {@code ativo=false}.
 */
public record AlterarStatusRequest(
        @NotNull(message = "O campo 'ativo' e obrigatorio.")
        Boolean ativo
) {
}
