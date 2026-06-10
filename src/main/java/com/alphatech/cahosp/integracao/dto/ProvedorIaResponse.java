package com.alphatech.cahosp.integracao.dto;

import com.alphatech.cahosp.integracao.dominio.PapelIa;
import com.alphatech.cahosp.integracao.dominio.ProvedorIa;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Provedor de IA do AI Gateway no front (RF-INT-06 / RF-SEG-04): papel, custo por 1k tokens,
 * volume mensal e flag de anonimizacao.
 */
public record ProvedorIaResponse(
        UUID id,
        String codigo,
        String nome,
        boolean ativo,
        PapelIa papel,
        BigDecimal custoPor1kTokens,
        long chamadasMes,
        boolean anonimizacao
) {

    public static ProvedorIaResponse de(ProvedorIa p) {
        return new ProvedorIaResponse(
                p.getId(),
                p.getCodigo(),
                p.getNome(),
                p.isAtivo(),
                p.getPapel(),
                p.getCustoPor1kTokens(),
                p.getChamadasMes(),
                p.isAnonimizacao());
    }
}
