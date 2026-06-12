package com.alphatech.cahosp.usuario.dto;

import com.alphatech.cahosp.usuario.dominio.Perfil;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Dados para atualizar um usuario (RF-ADM-01): nome, e-mail, perfil e unidade de lotacao.
 * A senha tem endpoint proprio ({@link RedefinirSenhaRequest}).
 *
 * <p>{@code unidadeId} e <strong>opcional</strong>: nulo desvincula o usuario de qualquer unidade.
 */
public record AtualizarUsuarioRequest(
        @NotBlank(message = "O nome e obrigatorio.")
        String nome,

        @NotBlank(message = "O e-mail e obrigatorio.")
        @Email(message = "E-mail invalido.")
        String email,

        @NotNull(message = "O perfil e obrigatorio.")
        Perfil perfil,

        UUID unidadeId
) {
}
