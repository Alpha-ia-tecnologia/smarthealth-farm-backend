package com.alphatech.cahosp.ia.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * Requisicao de chat ao AI Gateway (RF-INT-06): a sequencia de mensagens da conversa.
 */
public record ChatRequest(
        @NotEmpty(message = "Informe ao menos uma mensagem.")
        @Valid
        List<MensagemChat> mensagens
) {
}
