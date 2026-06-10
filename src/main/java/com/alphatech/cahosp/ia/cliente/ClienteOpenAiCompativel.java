package com.alphatech.cahosp.ia.cliente;

import com.alphatech.cahosp.ia.ClienteIa;
import com.alphatech.cahosp.ia.dto.MensagemChat;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * Base para provedores com API compativel com a do OpenAI ({@code POST /chat/completions}),
 * como DeepSeek e OpenAI (RF-INT-06). Concentra a montagem da requisicao e o parsing da resposta
 * (DRY); as subclasses fornecem nome, prioridade, URL, chave e modelo.
 */
public abstract class ClienteOpenAiCompativel implements ClienteIa {

    private final String nome;
    private final int prioridade;
    private final String apiKey;
    private final String modelo;
    private final RestClient restClient;

    protected ClienteOpenAiCompativel(String nome, int prioridade, String baseUrl, String apiKey,
                                      String modelo, RestClient.Builder builder) {
        this.nome = nome;
        this.prioridade = prioridade;
        this.apiKey = apiKey;
        this.modelo = modelo;
        this.restClient = builder.baseUrl(baseUrl).build();
    }

    @Override
    public String nome() {
        return nome;
    }

    @Override
    public String modelo() {
        return modelo;
    }

    @Override
    public int prioridade() {
        return prioridade;
    }

    @Override
    public boolean configurado() {
        return apiKey != null && !apiKey.isBlank();
    }

    @Override
    public String conversar(List<MensagemChat> mensagens) {
        List<Map<String, String>> messages = mensagens.stream()
                .map(m -> Map.of("role", m.papel(), "content", m.conteudo()))
                .toList();
        Map<String, Object> corpo = Map.of(
                "model", modelo,
                "messages", messages,
                "stream", false);

        JsonNode resposta = restClient.post()
                .uri("/chat/completions")
                .header("Authorization", "Bearer " + apiKey)
                .body(corpo)
                .retrieve()
                .body(JsonNode.class);

        if (resposta == null) {
            throw new IllegalStateException("Resposta vazia do provedor " + nome + ".");
        }
        return resposta.path("choices").path(0).path("message").path("content").asText();
    }
}
