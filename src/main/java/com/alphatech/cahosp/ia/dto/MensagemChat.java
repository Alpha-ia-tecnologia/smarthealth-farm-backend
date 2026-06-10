package com.alphatech.cahosp.ia.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Mensagem de uma conversa com o AI Gateway (RF-INT-06). {@code papel} segue a convencao dos
 * provedores compativeis com OpenAI ({@code system} / {@code user} / {@code assistant}).
 */
public record MensagemChat(
        @NotBlank(message = "O papel da mensagem e obrigatorio.")
        String papel,
        @NotBlank(message = "O conteudo da mensagem e obrigatorio.")
        String conteudo
) {
}
