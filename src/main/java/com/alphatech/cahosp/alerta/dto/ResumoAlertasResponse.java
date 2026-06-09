package com.alphatech.cahosp.alerta.dto;

/**
 * KPIs do painel de alertas (RF-ALE-04/05), espelhando os cards da AlertasPage do front:
 * <ul>
 *   <li>{@code abertos} — alertas em status Aberto ("Alertas abertos");</li>
 *   <li>{@code desabastecimento} — desabastecimentos ainda nao resolvidos ("Desabastecimento iminente");</li>
 *   <li>{@code vencimento} — vencimentos ainda nao resolvidos ("Risco de vencimento");</li>
 *   <li>{@code resolvidos} — alertas tratados/resolvidos ("Tratados").</li>
 * </ul>
 * {@code criticos} e {@code emTratamento} complementam a visao de severidade e tratamento.
 */
public record ResumoAlertasResponse(
        long abertos,
        long desabastecimento,
        long vencimento,
        long criticos,
        long emTratamento,
        long resolvidos,
        long total
) {
}
