package com.alphatech.cahosp.integracao;

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
 * Integracao EMSERH fim-a-fim (RF-INT) contra PostgreSQL real: conexoes, AI Gateway e KPIs.
 * Modulo somente leitura.
 */
@AutoConfigureMockMvc
@Transactional
class IntegracaoIT extends BaseIntegracaoPostgres {

    private static final String SENHA = "SenhaTeste123";

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private String token;

    @BeforeEach
    void preparar() throws Exception {
        String email = "op.int." + UUID.randomUUID().toString().substring(0, 8) + "@cahosp.local";
        usuarioRepository.save(new Usuario("Operador Int", email, passwordEncoder.encode(SENHA), Perfil.OPERADOR));
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
        mvc.perform(get("/integracoes"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NAO_AUTENTICADO"));
    }

    @Test
    @DisplayName("Lista 5 integracoes com status, modo, latencia e buffer")
    void listarIntegracoes() throws Exception {
        mvc.perform(get("/integracoes").header(HttpHeaders.AUTHORIZATION, bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(5))
                .andExpect(jsonPath("$.data[0].codigo").value("api-farmaweb"))
                .andExpect(jsonPath("$.data[0].status").value("Operacional"))
                .andExpect(jsonPath("$.data[0].modo").value("Online"))
                .andExpect(jsonPath("$.data[0].latenciaMs").isNumber())
                .andExpect(jsonPath("$.data[0].versao").exists());
    }

    @Test
    @DisplayName("Lista 3 provedores de IA com papel, custo e anonimizacao")
    void listarProvedoresIa() throws Exception {
        mvc.perform(get("/integracoes/provedores-ia").header(HttpHeaders.AUTHORIZATION, bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(3))
                .andExpect(jsonPath("$.data[0].codigo").value("ia-deepseek"))
                .andExpect(jsonPath("$.data[0].papel").value("Primário"))
                .andExpect(jsonPath("$.data[0].custoPor1kTokens").exists())
                .andExpect(jsonPath("$.data[0].anonimizacao").value(true));
    }

    @Test
    @DisplayName("Resumo traz operacionais, latencia media, buffer e provedores")
    void resumo() throws Exception {
        mvc.perform(get("/integracoes/resumo").header(HttpHeaders.AUTHORIZATION, bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.operacionais").value(3))
                .andExpect(jsonPath("$.data.totalIntegracoes").value(5))
                .andExpect(jsonPath("$.data.latenciaMediaMs").value(653))
                .andExpect(jsonPath("$.data.registrosBuffer").value(2458))
                .andExpect(jsonPath("$.data.provedoresIa").value(3));
    }
}
