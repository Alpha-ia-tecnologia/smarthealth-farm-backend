package com.alphatech.cahosp.usuario.dto;

import com.alphatech.cahosp.usuario.dominio.Perfil;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Dados para atualizar um usuario (RF-ADM-01): nome, e-mail e perfil.
 * A senha tem endpoint proprio ({@link RedefinirSenhaRequest}).
 */
public record AtualizarUsuarioRequest(
        @NotBlank(message = "O nome e obrigatorio.")
        String nome,

        @NotBlank(message = "O e-mail e obrigatorio.")
        @Email(message = "E-mail invalido.")
        String email,

        @NotNull(message = "O perfil e obrigatorio.")
        Perfil perfil
) {
}
