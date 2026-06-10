package com.alphatech.cahosp.seguranca.auditoria;

import com.alphatech.cahosp.recomendacao.RecomendacaoRepository;
import com.alphatech.cahosp.recomendacao.dominio.StatusRecomendacao;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.hamcrest.Matchers.everyItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Trilha de auditoria fim-a-fim (RF-SEG) contra PostgreSQL real: RBAC (so Gestor/TI), listagem com
 * filtros, KPIs e o registro automatico das acoes sensiveis (aprovacao de recomendacao) pelo
 * {@link RegistradorAuditoria}. {@code @Transactional} isola as mutacoes (rollback).
 */
@AutoConfigureMockMvc
@Transactional
class AuditoriaIT extends BaseIntegracaoPostgres {

    private static final String SENHA = "SenhaTeste123";

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    @Autowired private RecomendacaoRepository recomendacaoRepository;

    private String tokenGestor;
    private String tokenOperador;
    private UUID pendenteId;

    @BeforeEach
    void preparar() throws Exception {
        String sufixo = UUID.randomUUID().toString().substring(0, 8);
        String emailG = "gestor.aud." + sufixo + "@cahosp.local";
        String emailO = "op.aud." + sufixo + "@cahosp.local";
        usuarioRepository.save(new Usuario("Gestor Aud", emailG, passwordEncoder.encode(SENHA), Perfil.GESTOR));
        usuarioRepository.save(new Usuario("Operador Aud", emailO, passwordEncoder.encode(SENHA), Perfil.OPERADOR));
        tokenGestor = autenticar(emailG);
        tokenOperador = autenticar(emailO);

        pendenteId = recomendacaoRepository.findAll().stream()
                .filter(r -> r.getStatus() == StatusRecomendacao.PENDENTE)
                .map(r -> r.getId())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Seeder deveria ter recomendacoes pendentes."));
    }

    private String autenticar(String email) throws Exception {
        MvcResult login = mvc.perform(post("/auth/login")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("""
                                { "email": "%s", "password": "%s" }
                                """.formatted(email, SENHA)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(login.getResponse().getContentAsString())
                .path("data").path("token").asText();
    }

    private String gestor() {
        return "Bearer " + tokenGestor;
    }

    @Test
    @DisplayName("Sem token => 401")
    void semToken() throws Exception {
        mvc.perform(get("/seguranca/auditoria"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NAO_AUTENTICADO"));
    }

    @Test
    @DisplayName("Operador nao acessa a auditoria => 403 ACESSO_NEGADO")
    void operadorNegado() throws Exception {
        mvc.perform(get("/seguranca/auditoria").header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenOperador))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ACESSO_NEGADO"));
    }

    @Test
    @DisplayName("Gestor lista a trilha semeada (21 eventos) com os campos esperados")
    void listarComoGestor() throws Exception {
        mvc.perform(get("/seguranca/auditoria").header(HttpHeaders.AUTHORIZATION, gestor()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(21))
                .andExpect(jsonPath("$.data[0].usuario").exists())
                .andExpect(jsonPath("$.data[0].perfil").exists())
                .andExpect(jsonPath("$.data[0].categoria").exists())
                .andExpect(jsonPath("$.data[0].acao").exists())
                .andExpect(jsonPath("$.data[0].recurso").exists())
                .andExpect(jsonPath("$.data[0].baseLegal").exists())
                .andExpect(jsonPath("$.data[0].ip").exists())
                .andExpect(jsonPath("$.data[0].data").exists());
    }

    @Test
    @DisplayName("Filtro ?categoria=Inferência por IA devolve so as inferencias (assistidas por IA)")
    void filtroCategoria() throws Exception {
        mvc.perform(get("/seguranca/auditoria")
                        .param("categoria", "Inferência por IA")
                        .header(HttpHeaders.AUTHORIZATION, gestor()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(3))
                .andExpect(jsonPath("$.data[*].categoria", everyItem(org.hamcrest.Matchers.is("Inferência por IA"))))
                .andExpect(jsonPath("$.data[*].assistidoPorIA", everyItem(org.hamcrest.Matchers.is(true))));
    }

    @Test
    @DisplayName("Filtro ?assistidoPorIA=true devolve apenas eventos assistidos por IA")
    void filtroAssistidoPorIa() throws Exception {
        mvc.perform(get("/seguranca/auditoria")
                        .param("assistidoPorIA", "true")
                        .header(HttpHeaders.AUTHORIZATION, gestor()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(9))
                .andExpect(jsonPath("$.data[*].assistidoPorIA", everyItem(org.hamcrest.Matchers.is(true))));
    }

    @Test
    @DisplayName("Busca textual casa acao/recurso (ex.: 'lote')")
    void buscaTextual() throws Exception {
        mvc.perform(get("/seguranca/auditoria")
                        .param("busca", "lote")
                        .header(HttpHeaders.AUTHORIZATION, gestor()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(3))
                .andExpect(jsonPath("$.data[0].acao").value("Consultou histórico de lote"));
    }

    @Test
    @DisplayName("Resumo agrega total, assistidos por IA, com base legal e ultima atividade")
    void resumo() throws Exception {
        mvc.perform(get("/seguranca/auditoria/resumo").header(HttpHeaders.AUTHORIZATION, gestor()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(21))
                .andExpect(jsonPath("$.data.assistidosPorIa").value(9))
                .andExpect(jsonPath("$.data.comBaseLegal").value(21))
                .andExpect(jsonPath("$.data.ultimaAtividade").exists());
    }

    @Test
    @DisplayName("Aprovar recomendacao registra um evento na trilha (ator e recurso corretos) — RF-SEG-02")
    void aprovacaoGeraAuditoria() throws Exception {
        mvc.perform(post("/recomendacoes/" + pendenteId + "/aprovar")
                        .header(HttpHeaders.AUTHORIZATION, gestor()))
                .andExpect(status().isOk());

        mvc.perform(get("/seguranca/auditoria")
                        .param("busca", pendenteId.toString())
                        .header(HttpHeaders.AUTHORIZATION, gestor()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.data[0].categoria").value("Aprovação de recomendação"))
                .andExpect(jsonPath("$.data[0].usuario").value("Gestor Aud"))
                .andExpect(jsonPath("$.data[0].perfil").value("Gestor"))
                .andExpect(jsonPath("$.data[0].recurso").value("recomendacao:" + pendenteId))
                .andExpect(jsonPath("$.data[0].assistidoPorIA").value(false))
                .andExpect(jsonPath("$.data[0].ip").exists());
    }
}
