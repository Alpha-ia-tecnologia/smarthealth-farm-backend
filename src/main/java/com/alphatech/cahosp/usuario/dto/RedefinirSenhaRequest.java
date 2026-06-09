package com.alphatech.cahosp.usuario.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Redefine a senha de um usuario (RF-ADM-01). A nova senha vira hash BCrypt no servico.
 */
public record RedefinirSenhaRequest(
        @NotBlank(message = "A nova senha e obrigatoria.")
        @Size(min = 8, message = "A senha deve ter ao menos 8 caracteres.")
        String novaSenha
) {
}
