package com.alphatech.cahosp.seguranca.auditoria;

import com.alphatech.cahosp.seguranca.UsuarioAutenticado;
import com.alphatech.cahosp.usuario.dominio.Usuario;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Implementacao web de {@link ContextoAuditoria}: le o usuario autenticado do
 * {@code SecurityContextHolder} e o IP de origem da requisicao HTTP corrente. Degrada para
 * {@link AtorAuditoria#SISTEMA} (e IP nulo) quando nao ha autenticacao ou contexto de requisicao
 * (seeders, jobs internos). RF-SEG-02.
 */
@Component
public class ContextoAuditoriaWeb implements ContextoAuditoria {

    @Override
    public AtorAuditoria atorAtual() {
        Usuario usuario = usuarioAutenticado();
        String ip = ipDaRequisicao();
        if (usuario == null) {
            return new AtorAuditoria(null, AtorAuditoria.SISTEMA.nome(), null, ip);
        }
        return new AtorAuditoria(usuario.getId(), usuario.getNome(), usuario.getPerfil(), ip);
    }

    private Usuario usuarioAutenticado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()
                && auth.getPrincipal() instanceof UsuarioAutenticado ua) {
            return ua.getUsuario();
        }
        return null;
    }

    private String ipDaRequisicao() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            HttpServletRequest request = attrs.getRequest();
            String encaminhado = request.getHeader("X-Forwarded-For");
            if (encaminhado != null && !encaminhado.isBlank()) {
                return encaminhado.split(",")[0].trim();
            }
            return request.getRemoteAddr();
        }
        return null;
    }
}
