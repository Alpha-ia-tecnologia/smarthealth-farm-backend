package com.alphatech.cahosp.comum;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes unitarios do envelope de resposta (sem contexto Spring, sem Docker).
 */
class ApiResponseTest {

    @Test
    @DisplayName("ok() marca sucesso, carrega o payload e nao define total")
    void okDeveEnvelopar() {
        ApiResponse<String> resp = ApiResponse.ok("medicamento");

        assertThat(resp.success()).isTrue();
        assertThat(resp.data()).isEqualTo("medicamento");
        assertThat(resp.total()).isNull();
        assertThat(resp.error()).isNull();
        assertThat(resp.codigo()).isNull();
    }

    @Test
    @DisplayName("lista() inclui o total igual ao tamanho da colecao")
    void listaDeveCalcularTotal() {
        ApiResponse<List<String>> resp = ApiResponse.lista(List.of("a", "b", "c"));

        assertThat(resp.success()).isTrue();
        assertThat(resp.data()).containsExactly("a", "b", "c");
        assertThat(resp.total()).isEqualTo(3L);
    }

    @Test
    @DisplayName("erro() marca falha, sem data, com mensagem e codigo")
    void erroDevePreencherMensagemECodigo() {
        ApiResponse<Void> resp = ApiResponse.erro("Mensagem clara em portugues", "VALIDACAO");

        assertThat(resp.success()).isFalse();
        assertThat(resp.data()).isNull();
        assertThat(resp.error()).isEqualTo("Mensagem clara em portugues");
        assertThat(resp.codigo()).isEqualTo("VALIDACAO");
    }
}
