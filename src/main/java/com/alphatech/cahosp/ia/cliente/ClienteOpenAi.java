package com.alphatech.cahosp.ia.cliente;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Provedor de fallback do AI Gateway (RF-INT-06): OpenAI, via {@code /chat/completions}.
 */
@Component
public class ClienteOpenAi extends ClienteOpenAiCompativel {

    public ClienteOpenAi(
            @Value("${app.ia.openai-base-url:https://api.openai.com/v1}") String baseUrl,
            @Value("${app.ia.openai-api-key:}") String apiKey,
            @Value("${app.ia.openai-modelo:gpt-4o-mini}") String modelo,
            RestClient.Builder builder) {
        super("OpenAI", 2, baseUrl, apiKey, modelo, builder);
    }
}
