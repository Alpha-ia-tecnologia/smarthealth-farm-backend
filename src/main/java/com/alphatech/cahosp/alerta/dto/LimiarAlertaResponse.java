package com.alphatech.cahosp.alerta.dto;

import com.alphatech.cahosp.alerta.dominio.LimiarAlerta;

import java.time.Instant;

/** Configuracao atual dos limiares de disparo dos alertas (RF-ALE-03). */
public record LimiarAlertaResponse(
        int percentualEstoqueMinimo,
        int coberturaCriticaDias,
        int coberturaAltaDias,
        int antecedenciaVencimentoDias,
        int vencimentoCriticoDias,
        int vencimentoAltoDias,
        boolean desabastecimentoAtivo,
        boolean vencimentoAtivo,
        Instant atualizadoEm
) {

    public static LimiarAlertaResponse de(LimiarAlerta l) {
        return new LimiarAlertaResponse(
                l.getPercentualEstoqueMinimo(),
                l.getCoberturaCriticaDias(),
                l.getCoberturaAltaDias(),
                l.getAntecedenciaVencimentoDias(),
                l.getVencimentoCriticoDias(),
                l.getVencimentoAltoDias(),
                l.isDesabastecimentoAtivo(),
                l.isVencimentoAtivo(),
                l.getAtualizadoEm());
    }
}
