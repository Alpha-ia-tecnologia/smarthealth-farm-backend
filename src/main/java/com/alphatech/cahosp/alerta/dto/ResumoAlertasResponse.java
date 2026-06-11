package com.alphatech.cahosp.alerta.dto;

/**
 * KPIs do painel de alertas (RF-ALE-04/05), espelhando os cards da AlertasPage do front:
 * <ul>
 *   <li>{@code ativos} — alertas <strong>nao resolvidos</strong> (Aberto + Em tratamento), o card
 *       principal ("Alertas ativos");</li>
 *   <li>{@code desabastecimento} — desabastecimentos ainda nao resolvidos ("Desabastecimento iminente");</li>
 *   <li>{@code vencimento} — vencimentos ainda nao resolvidos ("Risco de vencimento");</li>
 *   <li>{@code resolvidos} — alertas tratados/resolvidos ("Tratados").</li>
 * </ul>
 *
 * <p>A fileira e <strong>somavel e coerente</strong>: como todo alerta e de um dos dois tipos,
 * {@code desabastecimento + vencimento == ativos}, e {@code ativos + resolvidos == total}.
 * {@code abertos}, {@code emTratamento} e {@code criticos} complementam a visao de status/severidade.
 */
public record ResumoAlertasResponse(
        long ativos,
        long abertos,
        long emTratamento,
        long desabastecimento,
        long vencimento,
        long criticos,
        long resolvidos,
        long total
) {
}
