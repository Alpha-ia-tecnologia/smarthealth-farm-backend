package com.alphatech.cahosp.ingestao;

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
 * Ingestao fim-a-fim (RF-DAD) contra PostgreSQL real: fontes, qualidade por categoria e KPIs.
 * Modulo somente leitura.
 */
@AutoConfigureMockMvc
@Transactional
class IngestaoIT extends BaseIntegracaoPostgres {

    private static final String SENHA = "SenhaTeste123";

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private String token;

    @BeforeEach
    void preparar() throws Exception {
        String email = "op.ing." + UUID.randomUUID().toString().substring(0, 8) + "@cahosp.local";
        usuarioRepository.save(new Usuario("Operador Ing", email, passwordEncoder.encode(SENHA), Perfil.OPERADOR));
        token = autenticar(email);
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

    private String bearer() {
        return "Bearer " + token;
    }

    @Test
    @DisplayName("Sem token => 401")
    void semToken() throws Exception {
        mvc.perform(get("/ingestao/fontes"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NAO_AUTENTICADO"));
    }

    @Test
    @DisplayName("Lista 6 fontes de dados com status, qualidade e procedencia")
    void listarFontes() throws Exception {
        mvc.perform(get("/ingestao/fontes").header(HttpHeaders.AUTHORIZATION, bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(6))
                .andExpect(jsonPath("$.data[0].codigo").value("f-sih"))
                .andExpect(jsonPath("$.data[0].nome").exists())
                .andExpect(jsonPath("$.data[0].status").value("Sincronizado"))
                .andExpect(jsonPath("$.data[0].registros").isNumber())
                .andExpect(jsonPath("$.data[0].qualidade").isNumber())
                .andExpect(jsonPath("$.data[0].procedencia").exists())
                .andExpect(jsonPath("$.data[0].ultimaIngestao").exists());
    }

    @Test
    @DisplayName("Lista qualidade por categoria (8 categorias)")
    void listarQualidade() throws Exception {
        mvc.perform(get("/ingestao/qualidade").header(HttpHeaders.AUTHORIZATION, bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(8))
                .andExpect(jsonPath("$.data[0].categoria").exists())
                .andExpect(jsonPath("$.data[0].maturidade").isNumber())
                .andExpect(jsonPath("$.data[0].completude").isNumber())
                .andExpect(jsonPath("$.data[0].consistencia").isNumber())
                .andExpect(jsonPath("$.data[0].granularidade").exists())
                .andExpect(jsonPath("$.data[0].lacunas").isNumber());
    }

    @Test
    @DisplayName("Resumo traz registros, fontes sincronizadas, qualidade media e LGPD ativa")
    void resumo() throws Exception {
        mvc.perform(get("/ingestao/resumo").header(HttpHeaders.AUTHORIZATION, bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.registrosIngeridos").value(2494912))
                .andExpect(jsonPath("$.data.totalFontes").value(6))
                .andExpect(jsonPath("$.data.fontesSincronizadas").value(4))
                .andExpect(jsonPath("$.data.qualidadeMedia").value(80))
                .andExpect(jsonPath("$.data.anonimizacaoAtiva").value(true));
    }
}
