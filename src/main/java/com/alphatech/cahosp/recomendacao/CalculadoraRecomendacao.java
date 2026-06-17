package com.alphatech.cahosp.recomendacao;

import com.alphatech.cahosp.medicamento.dominio.Criticidade;
import com.alphatech.cahosp.recomendacao.dominio.Prioridade;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Regras puras de dimensionamento de recomendacoes (RF-REC) — sem dependencia de banco, faceis
 * de testar. Espelha o algoritmo do front (src/data/index.ts):
 *
 * <ul>
 *   <li><strong>Reposicao:</strong> quantidade = {@code max(20, estoqueMaximo - quantidade)} —
 *       repoe ate o estoque maximo, com piso operacional.</li>
 *   <li><strong>Redistribuicao:</strong> quantidade = {@code max(10, round(nivelCritico*1.5 -
 *       quantidadeDestino))} — leva o destino a 1,5x o nivel critico.</li>
 *   <li><strong>Excedente:</strong> uma posicao e candidata a origem quando o saldo supera o
 *       dobro do nivel critico.</li>
 * </ul>
 */
@Component
public class CalculadoraRecomendacao {

    /** Piso de unidades para uma reposicao valer a pena operacionalmente. */
    private static final int PISO_REPOSICAO = 20;
    /** Piso de unidades para uma redistribuicao valer a pena. */
    private static final int PISO_REDISTRIBUICAO = 10;
    /** Alvo de cobertura do destino apos a redistribuicao (multiplicador do nivel critico). */
    private static final double ALVO_REDISTRIBUICAO = 1.5;
    /** Fator de folga para considerar uma posicao "excedente" (apta a doar). */
    private static final double FATOR_EXCEDENTE = 2.0;
    /**
     * Economia estimada (R$) por unidade transferida numa transferencia <strong>manual</strong>.
     * Valor fixo e filtrativo (MVP — nao ha metrica real de custo ainda); o front espelha esta
     * constante para mostrar a previa que muda com a quantidade. Ver {@code src/lib/recomendacoes.ts}.
     */
    public static final int FATOR_ECONOMIA_MANUAL = 12;

    /** Unidades a repor para restaurar o estoque maximo (RF-REC-01). */
    public int quantidadeReposicao(int estoqueMaximo, int quantidade) {
        return Math.max(PISO_REPOSICAO, estoqueMaximo - quantidade);
    }

    /** Unidades a transferir para levar o destino a {@value #ALVO_REDISTRIBUICAO}x o nivel critico. */
    public int quantidadeRedistribuicao(int nivelCritico, int quantidadeDestino) {
        long alvo = Math.round(nivelCritico * ALVO_REDISTRIBUICAO) - quantidadeDestino;
        if (alvo <= 0) {
            alvo = nivelCritico;
        }
        return (int) Math.max(PISO_REDISTRIBUICAO, alvo);
    }

    /** Uma posicao com este saldo e excedente (apta a doar) frente ao nivel critico? */
    public boolean ehExcedente(int quantidade, int nivelCritico) {
        return quantidade > nivelCritico * FATOR_EXCEDENTE;
    }

    /** Prioridade da reposicao pela criticidade do medicamento (RF-REC). */
    public Prioridade prioridadePorCriticidade(Criticidade criticidade) {
        return criticidade == Criticidade.ALTA ? Prioridade.ESSENCIAL : Prioridade.IMPORTANTE;
    }

    /** Economia estimada (R$) de uma transferencia manual: quantidade x {@value #FATOR_ECONOMIA_MANUAL}. */
    public BigDecimal economiaManual(int quantidade) {
        return BigDecimal.valueOf((long) quantidade * FATOR_ECONOMIA_MANUAL).setScale(2, RoundingMode.HALF_UP);
    }
}
