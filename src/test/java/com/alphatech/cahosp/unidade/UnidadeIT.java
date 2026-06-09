package com.alphatech.cahosp.unidade;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Catalogo de unidades fim-a-fim (RF-DAD-06) contra PostgreSQL real (Flyway + RBAC + seed).
 * Valida tambem que o {@link UnidadeSeeder} populou as 8 unidades, incluindo a CAHOSP hub.
 */
@AutoConfigureMockMvc
@Transactional // isola cada teste (rollback) — mantem o catalogo semeado estavel para as contagens
class UnidadeIT extends BaseIntegracaoPostgres {

    private static final String SENHA = "SenhaTeste123";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String tokenTi;
    private String tokenOperador;

    @BeforeEach
    void preparar() throws Exception {
        String sufixo = UUID.randomUUID().toString().substring(0, 8);
        String emailTi = "ti.uni." + sufixo + "@cahosp.local";
        String emailOp = "op.uni." + sufixo + "@cahosp.local";

        usuarioRepository.save(new Usuario("TI Teste Uni", emailTi, passwordEncoder.encode(SENHA), Perfil.TI));
        usuarioRepository.save(new Usuario("Operador Teste Uni", emailOp, passwordEncoder.encode(SENHA), Perfil.OPERADOR));

        tokenTi = autenticar(emailTi);
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
    @DisplayName("Seed populou 8 unidades, com CAHOSP hub=true")
    void seedPopulado() throws Exception {
        mvc.perform(get("/unidades").header(HttpHeaders.AUTHORIZATION, bearer(tokenOperador)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.total").isNumber());

        mvc.perform(get("/unidades?hub=true").header(HttpHeaders.AUTHORIZATION, bearer(tokenOperador)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].sigla").value("CAHOSP"))
                .andExpect(jsonPath("$.data[0].hub").value(true));

        mvc.perform(get("/unidades?hub=false").header(HttpHeaders.AUTHORIZATION, bearer(tokenOperador)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(7));
    }

    @Test
    @DisplayName("Filtros porte=Grande e busca=imperatriz devolvem o HRI")
    void filtrosListagem() throws Exception {
        mvc.perform(get("/unidades?busca=imperatriz")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenOperador)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].sigla").value("HRI"));

        mvc.perform(get("/unidades?porte=Grande&hub=false")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenOperador)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(2));
    }

    @Test
    @DisplayName("OPERADOR le; TI cria, atualiza e desativa")
    void fluxoTi() throws Exception {
        String sigla = "TEST" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        String corpo = """
                {"nome":"Hospital Teste","sigla":"%s","municipio":"Codó",
                 "porte":"Médio","leitos":120,"conectividade":"Intermitente",
                 "perfilDemografico":"Geral","hub":false}
                """.formatted(sigla);

        MvcResult criada = mvc.perform(post("/unidades")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenTi))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.sigla").value(sigla))
                .andExpect(jsonPath("$.data.porte").value("Médio"))
                .andExpect(jsonPath("$.data.ativo").value(true))
                .andReturn();
        String id = objectMapper.readTree(criada.getResponse().getContentAsString())
                .path("data").path("id").asText();

        // atualiza
        String atualizado = """
                {"nome":"Hospital Teste Atualizado","sigla":"%s","municipio":"Codó",
                 "porte":"Grande","leitos":180,"conectividade":"Estável",
                 "perfilDemografico":"Geral · ampliado","hub":false}
                """.formatted(sigla);
        mvc.perform(put("/unidades/" + id)
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenTi))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(atualizado))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.porte").value("Grande"))
                .andExpect(jsonPath("$.data.leitos").value(180));

        // desativa
        mvc.perform(patch("/unidades/" + id + "/status")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenTi))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ativo\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.ativo").value(false));
    }

    @Test
    @DisplayName("OPERADOR nao pode escrever — 403 ACESSO_NEGADO")
    void operadorNaoEscreve() throws Exception {
        mvc.perform(post("/unidades")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenOperador))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"X","sigla":"XX","municipio":"X","porte":"Pequeno","leitos":10,
                                 "conectividade":"Estável","perfilDemografico":"x","hub":false}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ACESSO_NEGADO"));
    }

    @Test
    @DisplayName("Sem token devolve 401 NAO_AUTENTICADO")
    void semToken() throws Exception {
        mvc.perform(get("/unidades"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NAO_AUTENTICADO"));
    }

    @Test
    @DisplayName("Sigla duplicada na criacao devolve 409 CONFLITO")
    void siglaDuplicada() throws Exception {
        mvc.perform(post("/unidades")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenTi))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Outro CAHOSP","sigla":"CAHOSP","municipio":"X",
                                 "porte":"Grande","leitos":0,"conectividade":"Estável",
                                 "perfilDemografico":"x","hub":true}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CONFLITO"));
    }

    @Test
    @DisplayName("Body invalido devolve 400 VALIDACAO")
    void bodyInvalido() throws Exception {
        mvc.perform(post("/unidades")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenTi))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"","sigla":"","municipio":"","porte":"Médio","leitos":-5,
                                 "conectividade":"Estável","perfilDemografico":"x","hub":false}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACAO"));
    }

    @Test
    @DisplayName("Detalhar id inexistente devolve 404 NAO_ENCONTRADO")
    void detalharInexistente() throws Exception {
        mvc.perform(get("/unidades/" + UUID.randomUUID())
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenOperador)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("NAO_ENCONTRADO"));
    }
}
