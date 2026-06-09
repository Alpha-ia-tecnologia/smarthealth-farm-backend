package com.alphatech.cahosp.usuario.dto;

import com.alphatech.cahosp.usuario.dominio.Perfil;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Dados para criar um usuario (RF-ADM-01). O {@code perfil} aceita o rotulo pt-BR
 * ("Operador"/"Gestor"/"TI") ou o nome da constante. A senha vira hash BCrypt no servico.
 */
public record CriarUsuarioRequest(
        @NotBlank(message = "O nome e obrigatorio.")
        String nome,

        @NotBlank(message = "O e-mail e obrigatorio.")
        @Email(message = "E-mail invalido.")
        String email,

        @NotNull(message = "O perfil e obrigatorio.")
        Perfil perfil,

        @NotBlank(message = "A senha e obrigatoria.")
        @Size(min = 8, message = "A senha deve ter ao menos 8 caracteres.")
        String senha
) {
}
