package com.alphatech.cahosp.ingestao.dto;

import com.alphatech.cahosp.ingestao.dominio.FonteDado;
import com.alphatech.cahosp.ingestao.dominio.StatusFonte;

import java.time.Instant;
import java.util.UUID;

/**
 * Linha da lista de fontes de dados do front (RF-DAD-02/07): origem, status, volume, qualidade
 * e procedencia.
 */
public record FonteDadoResponse(
        UUID id,
        String codigo,
        String nome,
        String geracao,
        StatusFonte status,
        Instant ultimaIngestao,
        long registros,
        int qualidade,
        String procedencia
) {

    public static FonteDadoResponse de(FonteDado fonte) {
        return new FonteDadoResponse(
                fonte.getId(),
                fonte.getCodigo(),
                fonte.getNome(),
                fonte.getGeracao(),
                fonte.getStatus(),
                fonte.getUltimaIngestao(),
                fonte.getRegistros(),
                fonte.getQualidade(),
                fonte.getProcedencia());
    }
}
