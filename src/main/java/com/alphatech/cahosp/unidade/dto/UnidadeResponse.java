package com.alphatech.cahosp.unidade.dto;

import com.alphatech.cahosp.unidade.dominio.Unidade;

import java.util.UUID;

/**
 * Representacao publica da unidade. {@code porte} e {@code conectividade} usam o rotulo pt-BR
 * esperado pelo frontend.
 */
public record UnidadeResponse(
        UUID id,
        String nome,
        String sigla,
        String municipio,
        String porte,
        int leitos,
        String conectividade,
        String perfilDemografico,
        boolean hub,
        boolean ativo
) {

    public static UnidadeResponse de(Unidade unidade) {
        return new UnidadeResponse(
                unidade.getId(),
                unidade.getNome(),
                unidade.getSigla(),
                unidade.getMunicipio(),
                unidade.getPorte().rotulo(),
                unidade.getLeitos(),
                unidade.getConectividade().rotulo(),
                unidade.getPerfilDemografico(),
                unidade.isHub(),
                unidade.isAtivo());
    }
}
