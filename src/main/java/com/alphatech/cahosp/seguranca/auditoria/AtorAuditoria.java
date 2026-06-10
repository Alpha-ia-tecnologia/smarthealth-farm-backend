package com.alphatech.cahosp.seguranca.auditoria;

import com.alphatech.cahosp.usuario.dominio.Perfil;

import java.util.UUID;

/**
 * Snapshot do ator de uma acao auditada, resolvido do contexto da requisicao (usuario autenticado
 * + IP de origem). Imutavel — e copiado para o {@code LogAuditoria}. RF-SEG-02.
 *
 * @param usuarioId id do usuario autenticado (nulo em acoes do sistema)
 * @param nome      nome do usuario (ou "Sistema")
 * @param perfil    perfil do usuario (nulo em acoes do sistema)
 * @param ip        IP de origem da requisicao (nulo fora de um contexto web)
 */
public record AtorAuditoria(UUID usuarioId, String nome, Perfil perfil, String ip) {

    /** Ator usado quando nao ha usuario autenticado (ex.: seeders, jobs internos). */
    public static final AtorAuditoria SISTEMA = new AtorAuditoria(null, "Sistema", null, null);
}
