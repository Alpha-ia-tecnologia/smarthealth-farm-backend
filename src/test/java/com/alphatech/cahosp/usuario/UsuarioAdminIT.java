package com.alphatech.cahosp.usuario;

import com.alphatech.cahosp.suporte.BaseIntegracaoPostgres;
import com.alphatech.cahosp.unidade.UnidadeRepository;
import com.alphatech.cahosp.unidade.dominio.Unidade;
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

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Administracao de usuarios fim-a-fim (RF-ADM-01) contra PostgreSQL real (Flyway + seguranca/RBAC).
 * Cria usuarios proprios (e-mails unicos por execucao) para nao colidir com o admin semeado nem
 * depender de ordem entre testes.
 */
@AutoConfigureMockMvc
class UsuarioAdminIT extends BaseIntegracaoPostgres {

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

    private String emailTi;
    private String emailOperador;
    private UUID idTi;
    private String tokenTi;
    private String tokenOperador;

    @BeforeEach
    void preparar() throws Exception {
        String sufixo = UUID.randomUUID().toString().substring(0, 8);
        emailTi = "ti." + sufixo + "@cahosp.local";
        emailOperador = "op." + sufixo + "@cahosp.local";

        Usuario ti = usuarioRepository.save(
                new Usuario("TI Teste", emailTi, passwordEncoder.encode(SENHA), Perfil.TI));
        idTi = ti.getId();
        usuarioRepository.save(
                new Usuario("Operador Teste", emailOperador, passwordEncoder.encode(SENHA), Perfil.OPERADOR));

        tokenTi = autenticar(emailTi, SENHA);
        tokenOperador = autenticar(emailOperador, SENHA);
    }

    private String autenticar(String email, String senha) throws Exception {
        MvcResult login = mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "email": "%s", "password": "%s" }
                                """.formatted(email, senha)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(login.getResponse().getContentAsString())
                .path("data").path("token").asText();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private String corpoCriar(String nome, String email, String perfil, String senha) {
        return """
                { "nome": "%s", "email": "%s", "perfil": "%s", "senha": "%s" }
                """.formatted(nome, email, perfil, senha);
    }

    @Test
    @DisplayName("TI cria usuario (201), lista, atualiza e desativa (200)")
    void fluxoCompletoTi() throws Exception {
        String email = "novo." + UUID.randomUUID().toString().substring(0, 8) + "@cahosp.local";

        MvcResult criado = mvc.perform(post("/admin/usuarios")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenTi))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoCriar("Novo Usuario", email, "Gestor", "senha1234")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.perfil").value("Gestor"))
                .andExpect(jsonPath("$.data.ativo").value(true))
                .andReturn();
        String id = objectMapper.readTree(criado.getResponse().getContentAsString())
                .path("data").path("id").asText();

        // lista (envelope de colecao com total)
        mvc.perform(get("/admin/usuarios")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenTi)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.total").isNumber());

        // atualiza
        mvc.perform(put("/admin/usuarios/" + id)
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenTi))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "nome": "Nome Editado", "email": "%s", "perfil": "Operador" }
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nome").value("Nome Editado"))
                .andExpect(jsonPath("$.data.perfil").value("Operador"));

        // desativa
        mvc.perform(patch("/admin/usuarios/" + id + "/status")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenTi))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"ativo\": false }"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.ativo").value(false));
    }

    @Test
    @DisplayName("TI cria usuario com unidade de lotacao (opcional) e a resposta traz sigla/nome")
    void criaComUnidade() throws Exception {
        Unidade unidade = unidadeRepository.findAll().get(0);
        String email = "lotado." + UUID.randomUUID().toString().substring(0, 8) + "@cahosp.local";

        mvc.perform(post("/admin/usuarios")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenTi))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "nome": "Lotado", "email": "%s", "perfil": "Operador",
                                  "senha": "senha1234", "unidadeId": "%s" }
                                """.formatted(email, unidade.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.unidadeId").value(unidade.getId().toString()))
                .andExpect(jsonPath("$.data.unidadeSigla").value(unidade.getSigla()))
                .andExpect(jsonPath("$.data.unidadeNome").value(unidade.getNome()));
    }

    @Test
    @DisplayName("Criar com unidadeId inexistente devolve 404 NAO_ENCONTRADO")
    void criaComUnidadeInexistente() throws Exception {
        String email = "semuni." + UUID.randomUUID().toString().substring(0, 8) + "@cahosp.local";

        mvc.perform(post("/admin/usuarios")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenTi))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "nome": "X", "email": "%s", "perfil": "Operador",
                                  "senha": "senha1234", "unidadeId": "%s" }
                                """.formatted(email, UUID.randomUUID())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("NAO_ENCONTRADO"));
    }

    @Test
    @DisplayName("OPERADOR recebe 403 ACESSO_NEGADO no GET e no POST")
    void operadorSemPermissao() throws Exception {
        mvc.perform(get("/admin/usuarios")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenOperador)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ACESSO_NEGADO"));

        mvc.perform(post("/admin/usuarios")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenOperador))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoCriar("X", "x@cahosp.local", "Operador", "senha1234")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ACESSO_NEGADO"));
    }

    @Test
    @DisplayName("Sem token devolve 401 NAO_AUTENTICADO")
    void semToken() throws Exception {
        mvc.perform(get("/admin/usuarios"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NAO_AUTENTICADO"));
    }

    @Test
    @DisplayName("Criar com e-mail ja existente devolve 409 CONFLITO")
    void emailDuplicado() throws Exception {
        mvc.perform(post("/admin/usuarios")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenTi))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoCriar("Repetido", emailOperador, "Operador", "senha1234")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CONFLITO"));
    }

    @Test
    @DisplayName("Body invalido devolve 400 VALIDACAO")
    void bodyInvalido() throws Exception {
        // nome em branco, e-mail invalido e senha curta
        mvc.perform(post("/admin/usuarios")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenTi))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "nome": "", "email": "nao-e-email", "perfil": "Operador", "senha": "123" }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACAO"));
    }

    @Test
    @DisplayName("TI desativando a si mesmo devolve 422 REGRA_NEGOCIO")
    void naoDesativaPropriaConta() throws Exception {
        mvc.perform(patch("/admin/usuarios/" + idTi + "/status")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenTi))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"ativo\": false }"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("REGRA_NEGOCIO"));
    }

    @Test
    @DisplayName("Detalhar id inexistente devolve 404 NAO_ENCONTRADO")
    void detalharInexistente() throws Exception {
        mvc.perform(get("/admin/usuarios/" + UUID.randomUUID())
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenTi)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("NAO_ENCONTRADO"));
    }
}
