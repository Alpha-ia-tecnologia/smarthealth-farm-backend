package com.alphatech.cahosp.medicamento;

import com.alphatech.cahosp.suporte.BaseIntegracaoPostgres;
import com.alphatech.cahosp.unidade.UnidadeRepository;
import com.alphatech.cahosp.unidade.dominio.Unidade;
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
 * Catalogo de medicamentos fim-a-fim (RF-DAD-06) contra PostgreSQL real. Confirma seed dos
 * 30 itens e os filtros mais usados pelo front (familia, criticidade, essencial, busca).
 */
@AutoConfigureMockMvc
@Transactional // isola cada teste (rollback) — mantem o catalogo semeado estavel para as contagens
class MedicamentoIT extends BaseIntegracaoPostgres {

    private static final String SENHA = "SenhaTeste123";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private UnidadeRepository unidadeRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String tokenTi;
    private String tokenOperador;

    @BeforeEach
    void preparar() throws Exception {
        String sufixo = UUID.randomUUID().toString().substring(0, 8);
        String emailTi = "ti.med." + sufixo + "@cahosp.local";
        String emailOp = "op.med." + sufixo + "@cahosp.local";

        usuarioRepository.save(new Usuario("TI Med", emailTi, passwordEncoder.encode(SENHA), Perfil.TI));
        usuarioRepository.save(new Usuario("Operador Med", emailOp, passwordEncoder.encode(SENHA), Perfil.OPERADOR));

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
    @DisplayName("Seed populou 30 medicamentos com MED-001 = Amoxicilina")
    void seedPopulado() throws Exception {
        mvc.perform(get("/medicamentos").header(HttpHeaders.AUTHORIZATION, bearer(tokenOperador)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.total").isNumber())
                .andExpect(jsonPath("$.data[0].codigo").value("MED-001"))
                .andExpect(jsonPath("$.data[0].familia").value("Antibióticos"));
    }

    @Test
    @DisplayName("Filtros familia + criticidade + essencial")
    void filtrosListagem() throws Exception {
        mvc.perform(get("/medicamentos?familia=Antibióticos")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenOperador)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(5));

        mvc.perform(get("/medicamentos?criticidade=Alta")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenOperador)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(11));

        mvc.perform(get("/medicamentos?essencial=false")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenOperador)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(3));

        mvc.perform(get("/medicamentos?busca=morfina")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenOperador)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].codigo").value("MED-007"));
    }

    @Test
    @DisplayName("Filtro ?unidadeId= lista so medicamentos com posicao na unidade")
    void filtroPorUnidade() throws Exception {
        UUID atendidaId = unidadeRepository.findAll().stream()
                .filter(u -> !u.isHub()).findFirst().orElseThrow().getId();
        UUID hubId = unidadeRepository.findAll().stream()
                .filter(Unidade::isHub).findFirst().orElseThrow().getId();

        // Unidade atendida recebe posicao de todos os 30 itens semeados (EstoqueSeeder).
        mvc.perform(get("/medicamentos").param("unidadeId", atendidaId.toString())
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenOperador)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(30));

        // Hub logistico (CAHOSP) nao consome diretamente — sem posicoes, lista vazia.
        mvc.perform(get("/medicamentos").param("unidadeId", hubId.toString())
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenOperador)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0));

        // Combina com os demais filtros (familia) dentro da unidade.
        mvc.perform(get("/medicamentos")
                        .param("unidadeId", atendidaId.toString())
                        .param("familia", "Antibióticos")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenOperador)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(5));
    }

    @Test
    @DisplayName("TI cria, atualiza e desativa medicamento")
    void fluxoTi() throws Exception {
        String codigo = "MED-T" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        String corpo = """
                {"codigo":"%s","nome":"Cefalexina 500mg","apresentacao":"Comprimido",
                 "familia":"Antibióticos","unidadeMedida":"cp","criticidade":"Média",
                 "essencial":true}
                """.formatted(codigo);

        MvcResult criado = mvc.perform(post("/medicamentos")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenTi))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.codigo").value(codigo))
                .andExpect(jsonPath("$.data.familia").value("Antibióticos"))
                .andExpect(jsonPath("$.data.criticidade").value("Média"))
                .andReturn();
        String id = objectMapper.readTree(criado.getResponse().getContentAsString())
                .path("data").path("id").asText();

        String atualizado = """
                {"codigo":"%s","nome":"Cefalexina 500mg","apresentacao":"Comprimido",
                 "familia":"Antibióticos","unidadeMedida":"cp","criticidade":"Alta",
                 "essencial":true}
                """.formatted(codigo);
        mvc.perform(put("/medicamentos/" + id)
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenTi))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(atualizado))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.criticidade").value("Alta"));

        mvc.perform(patch("/medicamentos/" + id + "/status")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenTi))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ativo\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.ativo").value(false));
    }

    @Test
    @DisplayName("OPERADOR nao pode escrever — 403 ACESSO_NEGADO")
    void operadorNaoEscreve() throws Exception {
        mvc.perform(post("/medicamentos")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenOperador))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"codigo":"MED-XX","nome":"x","apresentacao":"x",
                                 "familia":"Antibióticos","unidadeMedida":"cp",
                                 "criticidade":"Alta","essencial":true}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ACESSO_NEGADO"));
    }

    @Test
    @DisplayName("Sem token devolve 401")
    void semToken() throws Exception {
        mvc.perform(get("/medicamentos"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NAO_AUTENTICADO"));
    }

    @Test
    @DisplayName("Codigo duplicado devolve 409 CONFLITO")
    void codigoDuplicado() throws Exception {
        mvc.perform(post("/medicamentos")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenTi))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"codigo":"MED-001","nome":"Repetido","apresentacao":"x",
                                 "familia":"Antibióticos","unidadeMedida":"cp",
                                 "criticidade":"Alta","essencial":true}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CONFLITO"));
    }

    @Test
    @DisplayName("Body invalido devolve 400 VALIDACAO")
    void bodyInvalido() throws Exception {
        mvc.perform(post("/medicamentos")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenTi))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"codigo":"","nome":"","apresentacao":"",
                                 "familia":"Antibióticos","unidadeMedida":"",
                                 "criticidade":"Alta","essencial":false}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACAO"));
    }

    @Test
    @DisplayName("Detalhar id inexistente devolve 404")
    void detalharInexistente() throws Exception {
        mvc.perform(get("/medicamentos/" + UUID.randomUUID())
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenOperador)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("NAO_ENCONTRADO"));
    }
}
