package com.alphatech.cahosp.ia.cliente;

import com.alphatech.cahosp.ia.ClienteIa;
import com.alphatech.cahosp.ia.dto.MensagemChat;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * Provedor standby do AI Gateway (RF-INT-06): Google Gemini, via {@code :generateContent}
 * (formato distinto do compativel com OpenAI). Mapeia o papel {@code assistant} para {@code model}.
 */
@Component
public class ClienteGemini implements ClienteIa {

    private final String apiKey;
    private final String modelo;
    private final RestClient restClient;

    public ClienteGemini(
            @Value("${app.ia.gemini-base-url:https://generativelanguage.googleapis.com/v1beta}") String baseUrl,
            @Value("${app.ia.gemini-api-key:}") String apiKey,
            @Value("${app.ia.gemini-modelo:gemini-1.5-flash}") String modelo,
            RestClient.Builder builder) {
        this.apiKey = apiKey;
        this.modelo = modelo;
        this.restClient = builder.baseUrl(baseUrl).build();
    }

    @Override
    public String nome() {
        return "Google Gemini";
    }

    @Override
    public String modelo() {
        return modelo;
    }

    @Override
    public int prioridade() {
        return 3;
    }

    @Override
    public boolean configurado() {
        return apiKey != null && !apiKey.isBlank();
    }

    @Override
    public String conversar(List<MensagemChat> mensagens) {
        List<Map<String, Object>> contents = mensagens.stream()
                .map(m -> Map.of(
                        "role", "assistant".equalsIgnoreCase(m.papel()) ? "model" : "user",
                        "parts", List.of(Map.of("text", m.conteudo()))))
                .toList();

        JsonNode resposta = restClient.post()
                .uri("/models/{modelo}:generateContent?key={key}", modelo, apiKey)
                .body(Map.of("contents", contents))
                .retrieve()
                .body(JsonNode.class);

        if (resposta == null) {
            throw new IllegalStateException("Resposta vazia do provedor Gemini.");
        }
        return resposta.path("candidates").path(0).path("content")
                .path("parts").path(0).path("text").asText();
    }
}
