package com.alphatech.cahosp.insumo.dto;

import com.alphatech.cahosp.insumo.dominio.Insumo;

import java.util.UUID;

/**
 * Representacao publica do insumo. {@code categoria} e {@code criticidade} usam o rotulo
 * pt-BR esperado pelo frontend.
 */
public record InsumoResponse(
        UUID id,
        String codigo,
        String nome,
        String apresentacao,
        String categoria,
        String unidadeMedida,
        String criticidade,
        boolean essencial,
        boolean ativo
) {

    public static InsumoResponse de(Insumo insumo) {
        return new InsumoResponse(
                insumo.getId(),
                insumo.getCodigo(),
                insumo.getNome(),
                insumo.getApresentacao(),
                insumo.getCategoria().rotulo(),
                insumo.getUnidadeMedida(),
                insumo.getCriticidade().rotulo(),
                insumo.isEssencial(),
                insumo.isAtivo());
    }
}
