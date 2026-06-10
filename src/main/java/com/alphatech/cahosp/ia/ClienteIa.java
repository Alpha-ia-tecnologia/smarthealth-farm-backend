package com.alphatech.cahosp.ia;

import com.alphatech.cahosp.ia.dto.MensagemChat;

import java.util.List;

/**
 * Abstracao de um provedor de IA generativa (RF-INT-06). Permite trocar/estender provedores
 * (DeepSeek, OpenAI, Gemini...) sem alterar o {@link GatewayIaService} — princípios aberto/fechado,
 * substituicao de Liskov e inversao de dependencia (SOLID).
 *
 * <p>As implementacoes recebem mensagens <strong>ja anonimizadas</strong> (a anonimizacao e
 * responsabilidade do gateway, RF-SEG-04).
 */
public interface ClienteIa {

    /** Nome do provedor exibido na resposta (ex.: {@code DeepSeek}). */
    String nome();

    /** Modelo usado (ex.: {@code deepseek-chat}). */
    String modelo();

    /** Prioridade de selecao: menor = preferido (1 primario, 2 fallback, 3 standby). */
    int prioridade();

    /** Esta configurado (possui chave de API)? Provedores sem chave sao ignorados. */
    boolean configurado();

    /** Envia a conversa ao provedor e devolve o conteudo textual da resposta. */
    String conversar(List<MensagemChat> mensagens);
}
