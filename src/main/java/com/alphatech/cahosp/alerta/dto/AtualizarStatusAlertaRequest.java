package com.alphatech.cahosp.alerta.dto;

import com.alphatech.cahosp.alerta.dominio.StatusAlerta;
import jakarta.validation.constraints.NotNull;

/**
 * Requisicao de transicao de status de um alerta (RF-ALE-05). Aceita o rotulo pt-BR
 * (ex.: {@code "Em tratamento"}) ou o nome da constante.
 */
public record AtualizarStatusAlertaRequest(
        @NotNull(message = "O status e obrigatorio.")
        StatusAlerta status
) {
}
