package com.alphatech.cahosp.alerta.dto;

import com.alphatech.cahosp.alerta.dominio.Alerta;
import com.alphatech.cahosp.alerta.dominio.Severidade;
import com.alphatech.cahosp.alerta.dominio.StatusAlerta;
import com.alphatech.cahosp.alerta.dominio.TipoAlerta;
import com.alphatech.cahosp.usuario.dominio.Perfil;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Linha da tabela de alertas do front (RF-ALE-01/02/04): tipo, severidade, insumo, unidade,
 * mensagem, destinatarios (perfis) e status. {@code loteId}/{@code numeroLote} so vem em alertas
 * de vencimento.
 */
public record AlertaResponse(
        UUID id,
        TipoAlerta tipo,
        Severidade severidade,
        UUID insumoId,
        String insumoCodigo,
        String insumoNome,
        UUID unidadeId,
        String unidadeSigla,
        String unidadeNome,
        String mensagem,
        StatusAlerta status,
        List<Perfil> destinatarios,
        UUID loteId,
        String numeroLote,
        int diasParaEvento,
        Instant criadoEm
) {

    public static AlertaResponse de(Alerta a) {
        List<Perfil> destinatarios = a.getDestinatarios().stream()
                .sorted(Comparator.comparing(Enum::ordinal))
                .toList();
        UUID loteId = a.getLote() == null ? null : a.getLote().getId();
        String numeroLote = a.getLote() == null ? null : a.getLote().getNumeroLote();
        return new AlertaResponse(
                a.getId(),
                a.getTipo(),
                a.getSeveridade(),
                a.getInsumo().getId(),
                a.getInsumo().getCodigo(),
                a.getInsumo().getNome(),
                a.getUnidade().getId(),
                a.getUnidade().getSigla(),
                a.getUnidade().getNome(),
                a.getMensagem(),
                a.getStatus(),
                destinatarios,
                loteId,
                numeroLote,
                a.getDiasParaEvento(),
                a.getCriadoEm());
    }
}
