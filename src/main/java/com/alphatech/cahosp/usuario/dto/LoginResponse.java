package com.alphatech.cahosp.usuario.dto;

/**
 * Resultado do login: o usuario autenticado e o token JWT.
 */
public record LoginResponse(
        UsuarioResponse usuario,
        String token
) {
}
