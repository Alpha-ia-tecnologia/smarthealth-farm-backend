package com.alphatech.cahosp.medicamento.dto;

import jakarta.validation.constraints.NotNull;

/** Ativa ou desativa um medicamento. Sem exclusao fisica (LGPD/auditoria). RF-DAD-06. */
public record AlterarStatusMedicamentoRequest(
        @NotNull(message = "O campo 'ativo' e obrigatorio.")
        Boolean ativo
) {
}
