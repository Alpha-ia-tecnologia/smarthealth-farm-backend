package com.alphatech.cahosp.indicador.dto;

import com.alphatech.cahosp.indicador.CalculadoraIndicador;
import com.alphatech.cahosp.indicador.dominio.IndicadorMeta;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Indicador de desempenho do front (RF-IND-01..06): linha de base, valor atual, meta, histórico
 * e as derivacoes calculadas no servidor:
 * <ul>
 *   <li>{@code progresso} — % do caminho ate a meta (RF-IND-01);</li>
 *   <li>{@code atingiu} — meta alcancada (RF-IND-04);</li>
 *   <li>{@code variacaoPct} — variacao atual x base, sustenta o comparativo piloto x sistema
 *       atual (RF-IND-06); {@code null} quando a base e zero.</li>
 * </ul>
 * {@code unidade} e a unidade de medida ({@code %}, {@code dias}, {@code R$ mil}, ...).
 *
 * <p>{@code numeradorAbsoluto}/{@code denominadorAbsoluto}/{@code unidadeAbsoluta} dao o lastro em
 * numeros reais por tras de uma taxa (ex.: "9 de 80 itens essenciais" para 11,2% de
 * desabastecimento). {@code null} onde nao se aplica (unidade ja absoluta, ou MAPE).
 */
public record IndicadorResponse(
        UUID id,
        String codigo,
        String nome,
        String unidade,
        BigDecimal baseline,
        BigDecimal atual,
        BigDecimal meta,
        int metaReducaoPct,
        boolean melhorMenor,
        int progresso,
        boolean atingiu,
        Integer variacaoPct,
        BigDecimal numeradorAbsoluto,
        BigDecimal denominadorAbsoluto,
        String unidadeAbsoluta,
        List<PontoHistoricoResponse> historico
) {

    public static IndicadorResponse de(IndicadorMeta i, CalculadoraIndicador calculadora) {
        List<PontoHistoricoResponse> historico = i.getHistorico().stream()
                .map(PontoHistoricoResponse::de)
                .toList();
        return new IndicadorResponse(
                i.getId(),
                i.getCodigo(),
                i.getNome(),
                i.getUnidadeMedida(),
                i.getBaseline(),
                i.getAtual(),
                i.getMeta(),
                i.getMetaReducaoPct(),
                i.isMelhorMenor(),
                calculadora.progresso(i),
                calculadora.atingiu(i),
                calculadora.variacaoPct(i),
                i.getNumeradorAbsoluto(),
                i.getDenominadorAbsoluto(),
                i.getUnidadeAbsoluta(),
                historico);
    }
}
