package com.alphatech.cahosp.ia;

import com.alphatech.cahosp.ia.dto.ChatResponse;
import com.alphatech.cahosp.ia.dto.MensagemChat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

/**
 * Gateway de IA generativa (RF-INT-06 / RF-SEG-04). Orquestra os provedores ({@link ClienteIa})
 * por prioridade (DeepSeek → OpenAI → Gemini), com duas garantias:
 *
 * <ol>
 *   <li><strong>Anonimizacao antes do envio</strong> (RF-SEG-04): toda mensagem passa pelo
 *       {@link Anonimizador} antes de sair para qualquer provedor externo;</li>
 *   <li><strong>Modo demo resiliente</strong>: sem provedor configurado (sem chave de API) ou em
 *       caso de falha de todos, devolve uma resposta simulada ({@code mode: "demo"}) — nunca quebra.</li>
 * </ol>
 */
@Service
public class GatewayIaService {

    private static final Logger log = LoggerFactory.getLogger(GatewayIaService.class);

    private final List<ClienteIa> provedores;
    private final Anonimizador anonimizador;

    public GatewayIaService(List<ClienteIa> provedores, Anonimizador anonimizador) {
        // Ordena por prioridade (primario primeiro); imutavel apos a construcao.
        this.provedores = provedores.stream()
                .sorted(Comparator.comparingInt(ClienteIa::prioridade))
                .toList();
        this.anonimizador = anonimizador;
    }

    /** Processa a conversa: anonimiza, tenta os provedores por prioridade e cai para o modo demo. */
    public ChatResponse conversar(List<MensagemChat> mensagens) {
        List<MensagemChat> anonimizadas = mensagens.stream()
                .map(m -> new MensagemChat(m.papel(), anonimizador.anonimizar(m.conteudo())))
                .toList();

        for (ClienteIa provedor : provedores) {
            if (!provedor.configurado()) {
                continue;
            }
            try {
                String conteudo = provedor.conversar(anonimizadas);
                if (conteudo != null && !conteudo.isBlank()) {
                    return ChatResponse.online(conteudo, provedor.modelo(), provedor.nome());
                }
                log.warn("Provedor {} devolveu conteudo vazio; tentando o proximo.", provedor.nome());
            } catch (RuntimeException ex) {
                log.warn("Falha no provedor {} ({}); tentando o proximo.", provedor.nome(), ex.getMessage());
            }
        }
        return ChatResponse.demo(textoDemo(anonimizadas));
    }

    /** Resposta simulada (modo demo), citando a pergunta ja anonimizada (RF-SEG-04). */
    private String textoDemo(List<MensagemChat> anonimizadas) {
        String ultimaPergunta = anonimizadas.stream()
                .filter(m -> !"assistant".equalsIgnoreCase(m.papel()))
                .map(MensagemChat::conteudo)
                .reduce((primeiro, segundo) -> segundo)
                .orElse("");
        return "[Modo demonstração] O AI Gateway do Smart Health CAHOSP está sem provedor externo "
                + "configurado. Em produção, sua solicitação — anonimizada antes do envio (RF-SEG-04) — "
                + "seria encaminhada ao provedor primário (DeepSeek), com OpenAI e Gemini como "
                + "fallback. Pergunta recebida: \"" + ultimaPergunta + "\".";
    }
}
