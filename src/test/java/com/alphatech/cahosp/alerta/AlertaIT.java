package com.alphatech.cahosp.alerta;

import com.alphatech.cahosp.alerta.dominio.Alerta;
import com.alphatech.cahosp.alerta.dominio.StatusAlerta;
import com.alphatech.cahosp.suporte.BaseIntegracaoPostgres;
import com.alphatech.cahosp.usuario.UsuarioRepository;
import com.alphatech.cahosp.usuario.dominio.Perfil;
import com.alphatech.cahosp.usuario.dominio.Usuario;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Alertas fim-a-fim (RF-ALE) contra PostgreSQL real: listagem/filtros, KPIs, tratamento
 * (transicao de status com regra de terminal) e regeneracao pelo motor (RBAC de Gestor).
 * {@code @Transactional} isola as mutacoes (rollback).
 */
@AutoConfigureMockMvc
@Transactional
class AlertaIT extends BaseIntegracaoPostgres {

    private static final String SENHA = "SenhaTeste123";

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private AlertaRepository alertaRepository;

    private String tokenGestor;
    private String tokenOperador;
    private UUID alertaAbertoId;

    @BeforeEach
    void preparar() throws Exception {
        String sufixo = UUID.randomUUID().toString().substring(0, 8);
        String emailG = "gestor.ale." + sufixo + "@cahosp.local";
        String emailO = "op.ale." + sufixo + "@cahosp.local";
        usuarioRepository.save(new Usuario("Gestor Ale", emailG, passwordEncoder.encode(SENHA), Perfil.GESTOR));
        usuarioRepository.save(new Usuario("Operador Ale", emailO, passwordEncoder.encode(SENHA), Perfil.OPERADOR));
        tokenGestor = autenticar(emailG);
        tokenOperador = autenticar(emailO);

        alertaAbertoId = alertaRepository.findAll().stream()
                .filter(a -> a.getStatus() == StatusAlerta.ABERTO)
                .map(Alerta::getId)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Seeder deveria ter gerado alertas abertos."));
    }

    private String autenticar(String email) throws Exception {
        MvcResult login = mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "email": "%s", "password": "%s" }
                                """.formatted(email, SENHA)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(login.getResponse().getContentAsString())
                .path("data").path("token").asText();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    @Test
    @DisplayName("Sem token => 401")
    void semToken() throws Exception {
        mvc.perform(get("/alertas"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NAO_AUTENTICADO"));
    }

    @Test
    @DisplayName("Lista alertas com tipo, severidade, destinatarios e status")
    void listar() throws Exception {
        mvc.perform(get("/alertas").header(HttpHeaders.AUTHORIZATION, bearer(tokenOperador)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").isNumber())
                .andExpect(jsonPath("$.data[0].tipo").exists())
                .andExpect(jsonPath("$.data[0].severidade").exists())
                .andExpect(jsonPath("$.data[0].destinatarios").isArray())
                .andExpect(jsonPath("$.data[0].status").exists());
    }

    @Test
    @DisplayName("Filtro ?tipo=Vencimento (rotulo) devolve apenas vencimentos")
    void filtroTipo() throws Exception {
        mvc.perform(get("/alertas?tipo=Vencimento").header(HttpHeaders.AUTHORIZATION, bearer(tokenOperador)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].tipo").value("Vencimento"))
                .andExpect(jsonPath("$.data[0].loteId").exists());
    }

    @Test
    @DisplayName("Filtro ?status=Aberto devolve apenas abertos")
    void filtroStatus() throws Exception {
        mvc.perform(get("/alertas?status=Aberto").header(HttpHeaders.AUTHORIZATION, bearer(tokenOperador)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].status").value("Aberto"));
    }

    @Test
    @DisplayName("Paginacao: ?size=5 limita a pagina; total reflete o conjunto inteiro")
    void paginacao() throws Exception {
        mvc.perform(get("/alertas?page=0&size=5").header(HttpHeaders.AUTHORIZATION, bearer(tokenOperador)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()", lessThanOrEqualTo(5)))
                .andExpect(jsonPath("$.total").isNumber());
    }

    @Test
    @DisplayName("Limiares: GET devolve a configuracao vigente (defaults da V12)")
    void buscarLimiares() throws Exception {
        mvc.perform(get("/alertas/limiares").header(HttpHeaders.AUTHORIZATION, bearer(tokenOperador)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.percentualEstoqueMinimo").isNumber())
                .andExpect(jsonPath("$.data.antecedenciaVencimentoDias").isNumber())
                .andExpect(jsonPath("$.data.desabastecimentoAtivo").isBoolean())
                .andExpect(jsonPath("$.data.vencimentoAtivo").isBoolean());
    }

    @Test
    @DisplayName("Limiares: Gestor atualiza (200) e o novo valor passa a vigorar")
    void atualizarLimiaresComoGestor() throws Exception {
        String corpo = """
                {"percentualEstoqueMinimo":120,"coberturaCriticaDias":4,"coberturaAltaDias":9,
                 "antecedenciaVencimentoDias":75,"vencimentoCriticoDias":15,"vencimentoAltoDias":35,
                 "desabastecimentoAtivo":true,"vencimentoAtivo":true}
                """;
        mvc.perform(put("/alertas/limiares").header(HttpHeaders.AUTHORIZATION, bearer(tokenGestor))
                        .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.percentualEstoqueMinimo").value(120))
                .andExpect(jsonPath("$.data.antecedenciaVencimentoDias").value(75));

        mvc.perform(get("/alertas/limiares").header(HttpHeaders.AUTHORIZATION, bearer(tokenOperador)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.coberturaCriticaDias").value(4));
    }

    @Test
    @DisplayName("Limiares: Operador nao pode alterar => 403 ACESSO_NEGADO")
    void atualizarLimiaresComoOperador() throws Exception {
        String corpo = """
                {"percentualEstoqueMinimo":100,"coberturaCriticaDias":5,"coberturaAltaDias":10,
                 "antecedenciaVencimentoDias":60,"vencimentoCriticoDias":20,"vencimentoAltoDias":40,
                 "desabastecimentoAtivo":true,"vencimentoAtivo":true}
                """;
        mvc.perform(put("/alertas/limiares").header(HttpHeaders.AUTHORIZATION, bearer(tokenOperador))
                        .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ACESSO_NEGADO"));
    }

    @Test
    @DisplayName("Limiares: banda alta <= critica => 422 REGRA_NEGOCIO")
    void limiarIncoerente() throws Exception {
        String corpo = """
                {"percentualEstoqueMinimo":100,"coberturaCriticaDias":10,"coberturaAltaDias":10,
                 "antecedenciaVencimentoDias":60,"vencimentoCriticoDias":20,"vencimentoAltoDias":40,
                 "desabastecimentoAtivo":true,"vencimentoAtivo":true}
                """;
        mvc.perform(put("/alertas/limiares").header(HttpHeaders.AUTHORIZATION, bearer(tokenGestor))
                        .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("REGRA_NEGOCIO"));
    }

    @Test
    @DisplayName("Limiares: percentual fora da faixa => 400 VALIDACAO")
    void limiarForaDaFaixa() throws Exception {
        String corpo = """
                {"percentualEstoqueMinimo":500,"coberturaCriticaDias":5,"coberturaAltaDias":10,
                 "antecedenciaVencimentoDias":60,"vencimentoCriticoDias":20,"vencimentoAltoDias":40,
                 "desabastecimentoAtivo":true,"vencimentoAtivo":true}
                """;
        mvc.perform(put("/alertas/limiares").header(HttpHeaders.AUTHORIZATION, bearer(tokenGestor))
                        .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACAO"));
    }

    @Test
    @DisplayName("Toggle desligado vale no motor: vencimento inativo => 0 vencimentos gerados")
    void gerarComVencimentoDesligado() throws Exception {
        String corpo = """
                {"percentualEstoqueMinimo":100,"coberturaCriticaDias":5,"coberturaAltaDias":10,
                 "antecedenciaVencimentoDias":60,"vencimentoCriticoDias":20,"vencimentoAltoDias":40,
                 "desabastecimentoAtivo":true,"vencimentoAtivo":false}
                """;
        mvc.perform(put("/alertas/limiares").header(HttpHeaders.AUTHORIZATION, bearer(tokenGestor))
                        .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isOk());

        mvc.perform(post("/alertas/gerar").header(HttpHeaders.AUTHORIZATION, bearer(tokenGestor)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.vencimentoGerados").value(0));
    }

    @Test
    @DisplayName("Resumo traz os KPIs de alertas")
    void resumo() throws Exception {
        mvc.perform(get("/alertas/resumo").header(HttpHeaders.AUTHORIZATION, bearer(tokenOperador)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.ativos").isNumber())
                .andExpect(jsonPath("$.data.abertos").isNumber())
                .andExpect(jsonPath("$.data.emTratamento").isNumber())
                .andExpect(jsonPath("$.data.desabastecimento").isNumber())
                .andExpect(jsonPath("$.data.vencimento").isNumber())
                .andExpect(jsonPath("$.data.criticos").isNumber())
                .andExpect(jsonPath("$.data.resolvidos").isNumber())
                .andExpect(jsonPath("$.data.total").isNumber());
    }

    @Test
    @DisplayName("KPIs são coerentes e somáveis: ativos = abertos+emTratamento = desab.+venc.; +resolvidos = total")
    void resumoCoerente() throws Exception {
        MvcResult res = mvc.perform(get("/alertas/resumo").header(HttpHeaders.AUTHORIZATION, bearer(tokenOperador)))
                .andExpect(status().isOk())
                .andReturn();
        var data = objectMapper.readTree(res.getResponse().getContentAsString()).path("data");
        long ativos = data.path("ativos").asLong();
        long abertos = data.path("abertos").asLong();
        long emTratamento = data.path("emTratamento").asLong();
        long desabastecimento = data.path("desabastecimento").asLong();
        long vencimento = data.path("vencimento").asLong();
        long resolvidos = data.path("resolvidos").asLong();
        long total = data.path("total").asLong();

        org.assertj.core.api.Assertions.assertThat(ativos).isEqualTo(abertos + emTratamento);
        org.assertj.core.api.Assertions.assertThat(desabastecimento + vencimento).isEqualTo(ativos);
        org.assertj.core.api.Assertions.assertThat(ativos + resolvidos).isEqualTo(total);
    }

    @Test
    @DisplayName("Tratamento: Aberto -> Em tratamento -> Resolvido; resolvido e terminal (422)")
    void cicloDeTratamento() throws Exception {
        mvc.perform(patch("/alertas/" + alertaAbertoId + "/status")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenOperador))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"status\": \"Em tratamento\" }"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("Em tratamento"));

        mvc.perform(patch("/alertas/" + alertaAbertoId + "/status")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenOperador))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"status\": \"Resolvido\" }"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("Resolvido"));

        // Resolvido nao volta de status.
        mvc.perform(patch("/alertas/" + alertaAbertoId + "/status")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenOperador))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"status\": \"Em tratamento\" }"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("REGRA_NEGOCIO"));
    }

    @Test
    @DisplayName("Status invalido no body => 400 VALIDACAO")
    void statusInvalido() throws Exception {
        mvc.perform(patch("/alertas/" + alertaAbertoId + "/status")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenOperador))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"status\": \"Inexistente\" }"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Tratar alerta inexistente => 404 NAO_ENCONTRADO")
    void tratarInexistente() throws Exception {
        mvc.perform(patch("/alertas/" + UUID.randomUUID() + "/status")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenOperador))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"status\": \"Em tratamento\" }"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("NAO_ENCONTRADO"));
    }

    @Test
    @DisplayName("Gestor regenera os alertas (200) com KPIs da geracao")
    void gerarComoGestor() throws Exception {
        mvc.perform(post("/alertas/gerar").header(HttpHeaders.AUTHORIZATION, bearer(tokenGestor)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalAtivo").isNumber())
                .andExpect(jsonPath("$.data.desabastecimentoGerados").isNumber())
                .andExpect(jsonPath("$.data.vencimentoGerados").isNumber());
    }

    @Test
    @DisplayName("Operador nao pode regenerar => 403 ACESSO_NEGADO")
    void gerarComoOperador() throws Exception {
        mvc.perform(post("/alertas/gerar").header(HttpHeaders.AUTHORIZATION, bearer(tokenOperador)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ACESSO_NEGADO"));
    }

    @Test
    @DisplayName("Regenerar sem token => 401")
    void gerarSemToken() throws Exception {
        mvc.perform(post("/alertas/gerar"))
                .andExpect(status().isUnauthorized());
    }
}
