package com.alphatech.cahosp.medicamento.dto;

import com.alphatech.cahosp.medicamento.dominio.Medicamento;

import java.util.UUID;

/**
 * Representacao publica do medicamento. {@code familia} e {@code criticidade} usam o rotulo
 * pt-BR esperado pelo frontend.
 */
public record MedicamentoResponse(
        UUID id,
        String codigo,
        String nome,
        String apresentacao,
        String familia,
        String unidadeMedida,
        String criticidade,
        boolean essencial,
        boolean ativo
) {

    public static MedicamentoResponse de(Medicamento medicamento) {
        return new MedicamentoResponse(
                medicamento.getId(),
                medicamento.getCodigo(),
                medicamento.getNome(),
                medicamento.getApresentacao(),
                medicamento.getFamilia().rotulo(),
                medicamento.getUnidadeMedida(),
                medicamento.getCriticidade().rotulo(),
                medicamento.isEssencial(),
                medicamento.isAtivo());
    }
}
