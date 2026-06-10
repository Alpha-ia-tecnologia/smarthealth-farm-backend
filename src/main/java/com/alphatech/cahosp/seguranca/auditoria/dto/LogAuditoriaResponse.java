package com.alphatech.cahosp.seguranca.auditoria.dto;

import com.alphatech.cahosp.seguranca.auditoria.dominio.CategoriaAuditoria;
import com.alphatech.cahosp.seguranca.auditoria.dominio.LogAuditoria;
import com.alphatech.cahosp.usuario.dominio.Perfil;

import java.time.Instant;
import java.util.UUID;

/**
 * Linha da trilha de auditoria exibida na tela de Seguranca (RF-SEG-02). Espelha {@code LogAuditoria}
 * do front (data, usuario, perfil, acao, recurso, baseLegal, assistidoPorIA, ip), acrescido da
 * {@code categoria} tipada para filtro de governanca.
 */
public record LogAuditoriaResponse(
        UUID id,
        Instant data,
        String usuario,
        Perfil perfil,
        CategoriaAuditoria categoria,
        String acao,
        String recurso,
        String baseLegal,
        boolean assistidoPorIA,
        String ip
) {

    public static LogAuditoriaResponse de(LogAuditoria l) {
        return new LogAuditoriaResponse(
                l.getId(),
                l.getData(),
                l.getUsuarioNome(),
                l.getPerfil(),
                l.getCategoria(),
                l.getAcao(),
                l.getRecurso(),
                l.getBaseLegal(),
                l.isAssistidoPorIa(),
                l.getIp());
    }
}
