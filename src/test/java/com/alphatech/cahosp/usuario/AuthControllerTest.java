package com.alphatech.cahosp.usuario;

import com.alphatech.cahosp.seguranca.JwtService;
import com.alphatech.cahosp.seguranca.UsuarioDetailsService;
import com.alphatech.cahosp.usuario.dto.LoginResponse;
import com.alphatech.cahosp.usuario.dto.UsuarioResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testes de contrato HTTP do {@link AuthController} (slice MVC, sem DB).
 * Filtros de seguranca desativados — o RBAC/401 e exercitado no {@code AutenticacaoIT}.
 */
@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AutenticacaoService autenticacaoService;

    // O @WebMvcTest registra beans do tipo Filter (JwtAuthenticationFilter);
    // mockamos suas dependencias para o contexto do slice subir.
    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UsuarioDetailsService usuarioDetailsService;

    @Test
    @DisplayName("POST /auth/login valido devolve envelope com usuario e token")
    void loginOk() throws Exception {
        var usuario = new UsuarioResponse(UUID.randomUUID(), "Ana", "ana@emserh.ma.gov.br",
                "Gestor", true, null, null, null, Instant.now());
        when(autenticacaoService.login(any())).thenReturn(new LoginResponse(usuario, "jwt-fake"));

        mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "email": "ana@emserh.ma.gov.br", "password": "Senha123" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").value("jwt-fake"))
                .andExpect(jsonPath("$.data.usuario.email").value("ana@emserh.ma.gov.br"))
                .andExpect(jsonPath("$.data.usuario.perfil").value("Gestor"));
    }

    @Test
    @DisplayName("POST /auth/login com e-mail invalido devolve 400 VALIDACAO")
    void loginValidacao() throws Exception {
        mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "email": "nao-e-email", "password": "" }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.codigo").value("VALIDACAO"));
    }
}
