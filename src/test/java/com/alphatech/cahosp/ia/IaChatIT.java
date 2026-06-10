package com.alphatech.cahosp.ia;

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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * AI Gateway fim-a-fim (RF-INT-06 / RF-SEG-04). Sem chaves de API no ambiente de teste, o gateway
 * opera em modo demo — validando o fallback resiliente e a anonimizacao antes do envio.
 */
@AutoConfigureMockMvc
@Transactional
class IaChatIT extends BaseIntegracaoPostgres {

    private static final String SENHA = "SenhaTeste123";

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private String token;

    @BeforeEach
    void preparar() throws Exception {
        String email = "op.ia." + UUID.randomUUID().toString().substring(0, 8) + "@cahosp.local";
        usuarioRepository.save(new Usuario("Operador IA", email, passwordEncoder.encode(SENHA), Perfil.OPERADOR));
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
        mvc.perform(post("/ia/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "mensagens": [ { "papel": "user", "conteudo": "oi" } ] }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NAO_AUTENTICADO"));
    }

    @Test
    @DisplayName("Chat sem chave de API => responde em modo demo")
    void chatModoDemo() throws Exception {
        mvc.perform(post("/ia/chat")
                        .header(HttpHeaders.AUTHORIZATION, bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "mensagens": [ { "papel": "user", "conteudo": "Qual a previsao de ceftriaxona?" } ] }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.mode").value("demo"))
                .andExpect(jsonPath("$.data.provider").value("demo"))
                .andExpect(jsonPath("$.data.content").isNotEmpty());
    }

    @Test
    @DisplayName("Anonimiza dados pessoais antes de processar (RF-SEG-04)")
    void anonimiza() throws Exception {
        mvc.perform(post("/ia/chat")
                        .header(HttpHeaders.AUTHORIZATION, bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "mensagens": [ { "papel": "user", "conteudo": "Falar com joao@emserh.ma.gov.br" } ] }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("joao@emserh.ma.gov.br"))))
                .andExpect(jsonPath("$.data.content", org.hamcrest.Matchers.containsString("[email]")));
    }

    @Test
    @DisplayName("Sem mensagens => 400 VALIDACAO")
    void semMensagens() throws Exception {
        mvc.perform(post("/ia/chat")
                        .header(HttpHeaders.AUTHORIZATION, bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"mensagens\": [] }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACAO"));
    }
}
