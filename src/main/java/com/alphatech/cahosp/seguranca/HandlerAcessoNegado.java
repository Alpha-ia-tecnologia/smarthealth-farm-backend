package com.alphatech.cahosp.seguranca;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Responde 403 (no envelope da API) quando o usuario esta autenticado mas nao tem
 * permissao (perfil) para o recurso. RF-SEG (RBAC).
 */
@Component
public class HandlerAcessoNegado implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public HandlerAcessoNegado(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        RespostaSegurancaWriter.escrever(response, objectMapper,
                HttpStatus.FORBIDDEN, "Acesso negado para o seu perfil.", "ACESSO_NEGADO");
    }
}
