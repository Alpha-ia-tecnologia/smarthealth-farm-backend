package com.alphatech.cahosp.usuario.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Credenciais de login (RF-SEG). Login por e-mail.
 */
public record LoginRequest(
        @NotBlank(message = "O e-mail e obrigatorio.")
        @Email(message = "E-mail invalido.")
        String email,

        @NotBlank(message = "A senha e obrigatoria.")
        String password
) {
}
