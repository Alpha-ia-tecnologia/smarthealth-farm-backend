package com.alphatech.cahosp.ia.cliente;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Provedor primario do AI Gateway (RF-INT-06): DeepSeek, via API compativel com OpenAI.
 */
@Component
public class ClienteDeepSeek extends ClienteOpenAiCompativel {

    public ClienteDeepSeek(
            @Value("${app.ia.deepseek-base-url:https://api.deepseek.com}") String baseUrl,
            @Value("${app.ia.deepseek-api-key:}") String apiKey,
            @Value("${app.ia.deepseek-modelo:deepseek-chat}") String modelo,
            RestClient.Builder builder) {
        super("DeepSeek", 1, baseUrl, apiKey, modelo, builder);
    }
}
