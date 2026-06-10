package com.alphatech.cahosp.seguranca.auditoria;

/**
 * Resolve o ator da acao corrente (usuario autenticado + IP) a partir do contexto da requisicao.
 *
 * <p>Abstrai a dependencia do {@code SecurityContextHolder}/{@code RequestContextHolder} para que o
 * registrador de auditoria fique testavel por unidade (mock) sem montar um contexto web. RF-SEG.
 */
public interface ContextoAuditoria {

    /** Ator da requisicao atual, ou {@link AtorAuditoria#SISTEMA} quando nao ha usuario/contexto. */
    AtorAuditoria atorAtual();
}
