package com.alphatech.cahosp.seguranca;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Responde 401 (no envelope da API) quando uma requisicao sem autenticacao valida
 * tenta acessar recurso protegido.
 */
@Component
public class EntryPointNaoAutenticado implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public EntryPointNaoAutenticado(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        RespostaSegurancaWriter.escrever(response, objectMapper,
                HttpStatus.UNAUTHORIZED, "Autenticacao necessaria.", "NAO_AUTENTICADO");
    }
}
