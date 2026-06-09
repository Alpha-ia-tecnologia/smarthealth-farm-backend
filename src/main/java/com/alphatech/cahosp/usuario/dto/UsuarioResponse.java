package com.alphatech.cahosp.usuario.dto;

import com.alphatech.cahosp.usuario.dominio.Usuario;

import java.time.Instant;
import java.util.UUID;

/**
 * Representacao publica do usuario (nunca expoe a senha). O {@code perfil} usa o rotulo
 * pt-BR esperado pelo frontend (Operador/Gestor/TI).
 */
public record UsuarioResponse(
        UUID id,
        String nome,
        String email,
        String perfil,
        boolean ativo,
        Instant ultimoAcesso
) {

    public static UsuarioResponse de(Usuario usuario) {
        return new UsuarioResponse(
                usuario.getId(),
                usuario.getNome(),
                usuario.getEmail(),
                usuario.getPerfil().rotulo(),
                usuario.isAtivo(),
                usuario.getUltimoAcesso());
    }
}
