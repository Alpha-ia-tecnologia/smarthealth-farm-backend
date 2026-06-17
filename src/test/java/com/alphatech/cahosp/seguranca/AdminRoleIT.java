package com.alphatech.cahosp.seguranca;

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
 * Verifica que o perfil ADMIN (superusuario) acessa, via hierarquia de papeis, tanto rotas de
 * GESTOR quanto de TI — sem precisar de checagem caso a caso (RF-ADM / RF-SEG). Confirma tambem
 * que um OPERADOR continua barrado, garantindo que a hierarquia nao "abriu" para todos.
 */
@AutoConfigureMockMvc
@Transactional
class AdminRoleIT extends BaseIntegracaoPostgres {

    private static final String SENHA = "SenhaTeste123";

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private String tokenAdmin;
    private String tokenOperador;

    @BeforeEach
    void preparar() throws Exception {
        String sufixo = UUID.randomUUID().toString().substring(0, 8);
        String emailAdmin = "admin." + sufixo + "@cahosp.local";
        String emailOp = "op.adm." + sufixo + "@cahosp.local";
        usuarioRepository.save(new Usuario("Super Admin", emailAdmin, passwordEncoder.encode(SENHA), Perfil.ADMIN));
        usuarioRepository.save(new Usuario("Operador Adm", emailOp, passwordEncoder.encode(SENHA), Perfil.OPERADOR));
        tokenAdmin = autenticar(emailAdmin);
        tokenOperador = autenticar(emailOp);
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
    @DisplayName("ADMIN acessa rota de TI (GET /admin/usuarios) via hierarquia")
    void adminAcessaRotaDeTi() throws Exception {
        mvc.perform(get("/admin/usuarios").header(HttpHeaders.AUTHORIZATION, bearer(tokenAdmin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("ADMIN acessa rota de GESTOR (POST /alertas/gerar) via hierarquia")
    void adminAcessaRotaDeGestor() throws Exception {
        mvc.perform(post("/alertas/gerar").header(HttpHeaders.AUTHORIZATION, bearer(tokenAdmin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalAtivo").isNumber());
    }

    @Test
    @DisplayName("OPERADOR continua barrado nas rotas de TI e de Gestor (hierarquia nao liberou geral)")
    void operadorContinuaBarrado() throws Exception {
        mvc.perform(get("/admin/usuarios").header(HttpHeaders.AUTHORIZATION, bearer(tokenOperador)))
                .andExpect(status().isForbidden());
        mvc.perform(post("/alertas/gerar").header(HttpHeaders.AUTHORIZATION, bearer(tokenOperador)))
                .andExpect(status().isForbidden());
    }
}
