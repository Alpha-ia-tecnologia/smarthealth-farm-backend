package com.alphatech.cahosp.ia;

import com.alphatech.cahosp.ia.dto.ChatResponse;
import com.alphatech.cahosp.ia.dto.MensagemChat;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes unitarios da orquestracao do AI Gateway (RF-INT-06 / RF-SEG-04): selecao por prioridade,
 * fallback entre provedores, modo demo e anonimizacao antes do envio.
 */
class GatewayIaServiceTest {

    private final Anonimizador anonimizador = new AnonimizadorRegex();

    private static List<MensagemChat> pergunta(String conteudo) {
        return List.of(new MensagemChat("user", conteudo));
    }

    /** Stub de provedor que registra o que recebeu e responde/falha conforme configurado. */
    private static final class ClienteStub implements ClienteIa {
        private final String nome;
        private final int prioridade;
        private final boolean configurado;
        private final String resposta;
        private final boolean falha;
        private final List<MensagemChat> recebidas = new ArrayList<>();

        ClienteStub(String nome, int prioridade, boolean configurado, String resposta, boolean falha) {
            this.nome = nome;
            this.prioridade = prioridade;
            this.configurado = configurado;
            this.resposta = resposta;
            this.falha = falha;
        }

        @Override public String nome() { return nome; }
        @Override public String modelo() { return nome.toLowerCase() + "-model"; }
        @Override public int prioridade() { return prioridade; }
        @Override public boolean configurado() { return configurado; }

        @Override
        public String conversar(List<MensagemChat> mensagens) {
            recebidas.addAll(mensagens);
            if (falha) {
                throw new IllegalStateException("falha simulada");
            }
            return resposta;
        }
    }

    @Test
    @DisplayName("Sem provedor configurado => modo demo")
    void semProvedorConfigurado() {
        var gateway = new GatewayIaService(
                List.of(new ClienteStub("DeepSeek", 1, false, "x", false)), anonimizador);

        ChatResponse resposta = gateway.conversar(pergunta("Qual a cobertura?"));

        assertThat(resposta.mode()).isEqualTo("demo");
        assertThat(resposta.provider()).isEqualTo("demo");
        assertThat(resposta.content()).contains("[Modo demonstração]");
    }

    @Test
    @DisplayName("Provedor primario configurado responde em modo online")
    void primarioResponde() {
        var gateway = new GatewayIaService(
                List.of(new ClienteStub("DeepSeek", 1, true, "resposta do modelo", false)), anonimizador);

        ChatResponse resposta = gateway.conversar(pergunta("Pergunta"));

        assertThat(resposta.mode()).isEqualTo("online");
        assertThat(resposta.provider()).isEqualTo("DeepSeek");
        assertThat(resposta.model()).isEqualTo("deepseek-model");
        assertThat(resposta.content()).isEqualTo("resposta do modelo");
    }

    @Test
    @DisplayName("Falha do primario => fallback para o proximo provedor (por prioridade)")
    void fallbackEntreProvedores() {
        var primario = new ClienteStub("DeepSeek", 1, true, null, true);
        var fallback = new ClienteStub("OpenAI", 2, true, "resposta do fallback", false);
        // Passa fora de ordem para garantir que o gateway ordena por prioridade.
        var gateway = new GatewayIaService(List.of(fallback, primario), anonimizador);

        ChatResponse resposta = gateway.conversar(pergunta("Pergunta"));

        assertThat(resposta.mode()).isEqualTo("online");
        assertThat(resposta.provider()).isEqualTo("OpenAI");
        assertThat(resposta.content()).isEqualTo("resposta do fallback");
    }

    @Test
    @DisplayName("Anonimiza as mensagens antes de enviar ao provedor (RF-SEG-04)")
    void anonimizaAntesDeEnviar() {
        var provedor = new ClienteStub("DeepSeek", 1, true, "ok", false);
        var gateway = new GatewayIaService(List.of(provedor), anonimizador);

        gateway.conversar(pergunta("Contato ana@emserh.ma.gov.br sobre o lote"));

        String recebido = provedor.recebidas.get(0).conteudo();
        assertThat(recebido).doesNotContain("ana@emserh.ma.gov.br");
        assertThat(recebido).contains("[email]");
    }

    @Test
    @DisplayName("Todos os provedores falham => modo demo (nunca quebra)")
    void todosFalham() {
        var gateway = new GatewayIaService(
                List.of(new ClienteStub("DeepSeek", 1, true, null, true)), anonimizador);

        ChatResponse resposta = gateway.conversar(pergunta("Pergunta"));

        assertThat(resposta.mode()).isEqualTo("demo");
    }
}
