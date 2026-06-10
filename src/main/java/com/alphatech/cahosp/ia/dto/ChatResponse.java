package com.alphatech.cahosp.ia.dto;

/**
 * Resposta do AI Gateway (RF-INT-06). Os nomes dos campos seguem o contrato documentado no
 * CLAUDE.md ({@code content}/{@code model}/{@code mode}/{@code provider}) consumido pelo frontend.
 *
 * <ul>
 *   <li>{@code mode} — {@code "online"} (provedor externo respondeu) ou {@code "demo"} (sem chave
 *       configurada ou falha do provedor: resposta simulada);</li>
 *   <li>{@code provider} — provedor usado (ex.: {@code DeepSeek}) ou {@code demo}.</li>
 * </ul>
 */
public record ChatResponse(
        String content,
        String model,
        String mode,
        String provider
) {

    public static ChatResponse online(String content, String model, String provider) {
        return new ChatResponse(content, model, "online", provider);
    }

    public static ChatResponse demo(String content) {
        return new ChatResponse(content, "demo", "demo", "demo");
    }
}
