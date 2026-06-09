package com.alphatech.cahosp.previsao;

import com.alphatech.cahosp.previsao.dominio.Previsao;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Previsao fim-a-fim (RF-PRV) contra PostgreSQL real: listagem/filtros, serie, KPIs e a
 * recalibracao (RBAC de Gestor). {@code @Transactional} isola a recalibracao (rollback).
 */
@AutoConfigureMockMvc
@Transactional
class PrevisaoIT extends BaseIntegracaoPostgres {

    private static final String SENHA = "SenhaTeste123";

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private PrevisaoRepository previsaoRepository;

    private String tokenGestor;
    private String tokenOperador;
    private UUID medId;
    private UUID uniId;

    @BeforeEach
    void preparar() throws Exception {
        String sufixo = UUID.randomUUID().toString().substring(0, 8);
        String emailG = "gestor.prev." + sufixo + "@cahosp.local";
        String emailO = "op.prev." + sufixo + "@cahosp.local";
        usuarioRepository.save(new Usuario("Gestor Prev", emailG, passwordEncoder.encode(SENHA), Perfil.GESTOR));
        usuarioRepository.save(new Usuario("Operador Prev", emailO, passwordEncoder.encode(SENHA), Perfil.OPERADOR));
        tokenGestor = autenticar(emailG);
        tokenOperador = autenticar(emailO);

        Previsao previsao = previsaoRepository.findAll().get(0);
        medId = previsao.getMedicamento().getId();
        uniId = previsao.getUnidade().getId();
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
        mvc.perform(get("/previsoes"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NAO_AUTENTICADO"));
    }

    @Test
    @DisplayName("Lista previsoes com mape, drift e criticidade")
    void listar() throws Exception {
        mvc.perform(get("/previsoes").header(HttpHeaders.AUTHORIZATION, bearer(tokenOperador)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").isNumber())
                .andExpect(jsonPath("$.data[0].mape").exists())
                .andExpect(jsonPath("$.data[0].drift").exists())
                .andExpect(jsonPath("$.data[0].criticidade").exists());
    }

    @Test
    @DisplayName("Filtro ?drift=Estável (rotulo) devolve apenas estaveis")
    void filtroDrift() throws Exception {
        mvc.perform(get("/previsoes?drift=Estável").header(HttpHeaders.AUTHORIZATION, bearer(tokenOperador)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].drift").value("Estável"));
    }

    @Test
    @DisplayName("Resumo traz os KPIs da previsao")
    void resumo() throws Exception {
        mvc.perform(get("/previsoes/resumo").header(HttpHeaders.AUTHORIZATION, bearer(tokenOperador)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.mapeMedio").isNumber())
                .andExpect(jsonPath("$.data.previsoesAtivas").isNumber())
                .andExpect(jsonPath("$.data.totalCriticos").isNumber())
                .andExpect(jsonPath("$.data.itensComDesvio").isNumber());
    }

    @Test
    @DisplayName("Drill-down traz a serie com 15 pontos (12 historico + 3 previsao)")
    void detalhar() throws Exception {
        mvc.perform(get("/previsoes/" + medId + "/" + uniId).header(HttpHeaders.AUTHORIZATION, bearer(tokenOperador)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.previsao.medicamentoId").value(medId.toString()))
                .andExpect(jsonPath("$.data.serie.length()").value(15))
                .andExpect(jsonPath("$.data.serie[0].periodo").exists());
    }

    @Test
    @DisplayName("Drill-down inexistente => 404")
    void detalharInexistente() throws Exception {
        mvc.perform(get("/previsoes/" + UUID.randomUUID() + "/" + UUID.randomUUID())
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenOperador)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("NAO_ENCONTRADO"));
    }

    @Test
    @DisplayName("Gestor recalibra (200) e estabiliza o drift da previsao")
    void recalibrarComoGestor() throws Exception {
        mvc.perform(post("/previsoes/recalibrar").header(HttpHeaders.AUTHORIZATION, bearer(tokenGestor)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.recalibradas").isNumber());

        mvc.perform(get("/previsoes/" + medId + "/" + uniId).header(HttpHeaders.AUTHORIZATION, bearer(tokenGestor)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.previsao.drift").value("Estável"));
    }

    @Test
    @DisplayName("Operador nao pode recalibrar => 403 ACESSO_NEGADO")
    void recalibrarComoOperador() throws Exception {
        mvc.perform(post("/previsoes/recalibrar").header(HttpHeaders.AUTHORIZATION, bearer(tokenOperador)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ACESSO_NEGADO"));
    }

    @Test
    @DisplayName("Recalibrar sem token => 401")
    void recalibrarSemToken() throws Exception {
        mvc.perform(post("/previsoes/recalibrar"))
                .andExpect(status().isUnauthorized());
    }
}
