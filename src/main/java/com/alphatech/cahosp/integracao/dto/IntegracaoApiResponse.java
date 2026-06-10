package com.alphatech.cahosp.integracao.dto;

import com.alphatech.cahosp.integracao.dominio.IntegracaoApi;
import com.alphatech.cahosp.integracao.dominio.ModoIntegracao;
import com.alphatech.cahosp.integracao.dominio.StatusIntegracao;

import java.time.Instant;
import java.util.UUID;

/**
 * Conexao/sincronizacao do front (RF-INT-01/02/04/05): API versionada, situacao, latencia, modo
 * de operacao e volume em buffer offline.
 */
public record IntegracaoApiResponse(
        UUID id,
        String codigo,
        String nome,
        String versao,
        StatusIntegracao status,
        int latenciaMs,
        Instant ultimaSync,
        ModoIntegracao modo,
        int registrosBuffer
) {

    public static IntegracaoApiResponse de(IntegracaoApi i) {
        return new IntegracaoApiResponse(
                i.getId(),
                i.getCodigo(),
                i.getNome(),
                i.getVersao(),
                i.getStatus(),
                i.getLatenciaMs(),
                i.getUltimaSync(),
                i.getModo(),
                i.getRegistrosBuffer());
    }
}
