package com.alphatech.cahosp.seguranca;

import com.alphatech.cahosp.comum.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Escreve erros de seguranca (401/403) no mesmo envelope {@link ApiResponse} da API,
 * para o frontend tratar de forma uniforme.
 */
final class RespostaSegurancaWriter {

    private RespostaSegurancaWriter() {
    }

    static void escrever(HttpServletResponse response, ObjectMapper objectMapper,
                         HttpStatus status, String mensagem, String codigo) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getWriter(), ApiResponse.erro(mensagem, codigo));
    }
}
