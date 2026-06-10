package com.alphatech.cahosp.seguranca.auditoria;

import com.alphatech.cahosp.seguranca.auditoria.dominio.CategoriaAuditoria;

/**
 * Porta para registrar acoes sensiveis na trilha de auditoria (RF-SEG-01/02/03).
 *
 * <p>Interface pequena e focada (ISP): os demais dominios (recomendacao, previsao, alerta, IA)
 * dependem desta abstracao (DIP) e disparam o registro sem conhecer persistencia, contexto de
 * seguranca nem IP — tudo isso e resolvido pela implementacao. Mantem os controllers e a regra de
 * negocio desacoplados da auditoria, e facil de mockar/stubar nos testes.
 */
public interface RegistradorAuditoria {

    /**
     * Registra uma acao usando os valores-padrao da categoria (acao, base legal e flag de IA).
     *
     * @param categoria tipo da acao sensivel
     * @param recurso   identificador do recurso afetado (ex.: {@code "recomendacao:<uuid>"})
     */
    void registrar(CategoriaAuditoria categoria, String recurso);

    /**
     * Registra uma acao detalhando acao, base legal e se foi assistida por IA.
     *
     * @param categoria      tipo da acao sensivel
     * @param acao           descricao legivel da acao
     * @param recurso        identificador do recurso afetado
     * @param baseLegal      base legal LGPD aplicavel (RF-SEG-03)
     * @param assistidoPorIa indica se a decisao foi assistida por IA (RF-SEG-02/04)
     */
    void registrar(CategoriaAuditoria categoria, String acao, String recurso,
                   String baseLegal, boolean assistidoPorIa);
}
