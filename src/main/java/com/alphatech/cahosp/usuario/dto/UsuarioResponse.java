package com.alphatech.cahosp.usuario.dto;

import com.alphatech.cahosp.unidade.dominio.Unidade;
import com.alphatech.cahosp.usuario.dominio.Usuario;

import java.time.Instant;
import java.util.UUID;

/**
 * Representacao publica do usuario (nunca expoe a senha). O {@code perfil} usa o rotulo
 * pt-BR esperado pelo frontend (Operador/Gestor/TI).
 *
 * <p>A unidade de lotacao e <strong>opcional</strong> (RF-ADM-01): quando o usuario nao tem
 * unidade, {@code unidadeId}/{@code unidadeSigla}/{@code unidadeNome} vem nulos.
 */
public record UsuarioResponse(
        UUID id,
        String nome,
        String email,
        String perfil,
        boolean ativo,
        UUID unidadeId,
        String unidadeSigla,
        String unidadeNome,
        Instant ultimoAcesso
) {

    public static UsuarioResponse de(Usuario usuario) {
        Unidade unidade = usuario.getUnidade();
        return new UsuarioResponse(
                usuario.getId(),
                usuario.getNome(),
                usuario.getEmail(),
                usuario.getPerfil().rotulo(),
                usuario.isAtivo(),
                unidade == null ? null : unidade.getId(),
                unidade == null ? null : unidade.getSigla(),
                unidade == null ? null : unidade.getNome(),
                usuario.getUltimoAcesso());
    }
}
