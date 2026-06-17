package com.alphatech.cahosp.usuario;

import com.alphatech.cahosp.suporte.BaseIntegracaoPostgres;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Fluxo de autenticacao fim-a-fim contra PostgreSQL real (Flyway + seguranca real).
 * Usa o administrador semeado pelo {@link AdminSeeder} (credenciais do application-test.yml).
 */
@AutoConfigureMockMvc
class AutenticacaoIT extends BaseIntegracaoPostgres {

    private static final String ADMIN_EMAIL = "admin.teste@cahosp.local";
    private static final String ADMIN_SENHA = "SenhaTeste123";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String corpoLogin(String email, String senha) {
        return """
                { "email": "%s", "password": "%s" }
                """.formatted(email, senha);
    }

    @Test
    @DisplayName("Login com credenciais do admin devolve token e perfil Admin")
    void loginAdminOk() throws Exception {
        mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoLogin(ADMIN_EMAIL, ADMIN_SENHA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andExpect(jsonPath("$.data.usuario.email").value(ADMIN_EMAIL))
                .andExpect(jsonPath("$.data.usuario.perfil").value("Admin"));
    }

    @Test
    @DisplayName("Senha errada devolve 401 CREDENCIAIS_INVALIDAS")
    void loginSenhaErrada() throws Exception {
        mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoLogin(ADMIN_EMAIL, "senha-errada")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.codigo").value("CREDENCIAIS_INVALIDAS"));
    }

    @Test
    @DisplayName("GET /auth/me sem token devolve 401 NAO_AUTENTICADO")
    void meSemToken() throws Exception {
        mvc.perform(get("/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NAO_AUTENTICADO"));
    }

    @Test
    @DisplayName("GET /auth/me com token devolve o usuario autenticado")
    void meComToken() throws Exception {
        MvcResult login = mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoLogin(ADMIN_EMAIL, ADMIN_SENHA)))
                .andExpect(status().isOk())
                .andReturn();
        String token = objectMapper.readTree(login.getResponse().getContentAsString())
                .path("data").path("token").asText();

        mvc.perform(get("/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value(ADMIN_EMAIL))
                .andExpect(jsonPath("$.data.perfil").value("Admin"));
    }

    @Test
    @DisplayName("Login com e-mail invalido devolve 400 VALIDACAO")
    void loginValidacao() throws Exception {
        mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoLogin("nao-e-email", "x")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACAO"));
    }
}
