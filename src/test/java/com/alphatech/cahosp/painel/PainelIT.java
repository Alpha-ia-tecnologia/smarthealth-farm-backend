package com.alphatech.cahosp.painel;

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
 * Painel fim-a-fim (RF-DASH) contra PostgreSQL real: dashboard gerencial e painel operacional.
 * Modulo somente leitura — qualquer autenticado pode consultar.
 */
@AutoConfigureMockMvc
@Transactional
class PainelIT extends BaseIntegracaoPostgres {

    private static final String SENHA = "SenhaTeste123";

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private String token;

    @BeforeEach
    void preparar() throws Exception {
        String email = "op.painel." + UUID.randomUUID().toString().substring(0, 8) + "@cahosp.local";
        usuarioRepository.save(new Usuario("Operador Painel", email, passwordEncoder.encode(SENHA), Perfil.OPERADOR));
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
        mvc.perform(get("/painel"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NAO_AUTENTICADO"));
    }

    @Test
    @DisplayName("Dashboard gerencial traz totais, cobertura, serie agregada, alertas e recomendacoes")
    void dashboard() throws Exception {
        mvc.perform(get("/painel").header(HttpHeaders.AUTHORIZATION, bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totais.medicamentos").value(30))
                .andExpect(jsonPath("$.data.totais.unidades").value(7))
                .andExpect(jsonPath("$.data.totais.alertasAbertos").isNumber())
                .andExpect(jsonPath("$.data.totais.economiaPotencial").exists())
                .andExpect(jsonPath("$.data.coberturaPorUnidade").isArray())
                .andExpect(jsonPath("$.data.coberturaPorUnidade.length()").value(7))
                .andExpect(jsonPath("$.data.coberturaPorUnidade[0].nome").exists())
                .andExpect(jsonPath("$.data.coberturaPorUnidade[0].valor").isNumber())
                .andExpect(jsonPath("$.data.coberturaPorUnidade[0].status").exists())
                .andExpect(jsonPath("$.data.serieAgregada.medicamentoCodigo").exists())
                .andExpect(jsonPath("$.data.serieAgregada.serie").isArray())
                .andExpect(jsonPath("$.data.serieAgregada.serie.length()").value(15))
                .andExpect(jsonPath("$.data.alertasRecentes").isArray())
                .andExpect(jsonPath("$.data.recomendacoesPendentes").isArray());
    }

    @Test
    @DisplayName("Painel operacional traz unidades, alertas ativos e recomendacoes em aberto")
    void operacional() throws Exception {
        mvc.perform(get("/painel/operacional").header(HttpHeaders.AUTHORIZATION, bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totais.medicamentos").value(30))
                .andExpect(jsonPath("$.data.unidades").isArray())
                .andExpect(jsonPath("$.data.unidades.length()").value(7))
                .andExpect(jsonPath("$.data.unidades[0].sigla").exists())
                .andExpect(jsonPath("$.data.unidades[0].cobertura").isNumber())
                .andExpect(jsonPath("$.data.unidades[0].statusUnidade").exists())
                .andExpect(jsonPath("$.data.unidades[0].conectividade").exists())
                .andExpect(jsonPath("$.data.alertasAtivos").isArray())
                .andExpect(jsonPath("$.data.recomendacoesAbertas").isArray());
    }
}
