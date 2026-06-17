package com.alphatech.cahosp.painel;

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
    @Autowired private UnidadeRepository unidadeRepository;
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
                .andExpect(jsonPath("$.data.totais.insumos").value(30))
                .andExpect(jsonPath("$.data.totais.unidades").value(7))
                .andExpect(jsonPath("$.data.totais.alertasAbertos").isNumber())
                // "ativos" (abertos + em tratamento) bate com a metrica da tela de alertas:
                // nao resolvidos = desabastecimento + vencimento.
                .andExpect(jsonPath("$.data.totais.alertasAtivos").isNumber())
                .andExpect(jsonPath("$.data.totais.economiaPotencial").exists())
                .andExpect(jsonPath("$.data.coberturaPorUnidade").isArray())
                .andExpect(jsonPath("$.data.coberturaPorUnidade.length()").value(7))
                .andExpect(jsonPath("$.data.coberturaPorUnidade[0].nome").exists())
                .andExpect(jsonPath("$.data.coberturaPorUnidade[0].valor").isNumber())
                .andExpect(jsonPath("$.data.coberturaPorUnidade[0].status").exists())
                .andExpect(jsonPath("$.data.serieAgregada.insumoCodigo").exists())
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
                .andExpect(jsonPath("$.data.totais.insumos").value(30))
                .andExpect(jsonPath("$.data.unidades").isArray())
                .andExpect(jsonPath("$.data.unidades.length()").value(7))
                .andExpect(jsonPath("$.data.unidades[0].sigla").exists())
                .andExpect(jsonPath("$.data.unidades[0].cobertura").isNumber())
                .andExpect(jsonPath("$.data.unidades[0].statusUnidade").exists())
                .andExpect(jsonPath("$.data.unidades[0].conectividade").exists())
                .andExpect(jsonPath("$.data.alertasAtivos").isArray())
                .andExpect(jsonPath("$.data.recomendacoesAbertas").isArray());
    }

    @Test
    @DisplayName("Dashboard com ?unidadeId= filtra os totais, mas a cobertura segue a rede inteira")
    void dashboardFiltradoPorUnidade() throws Exception {
        UUID unidadeId = unidadeAtendida();
        mvc.perform(get("/painel").param("unidadeId", unidadeId.toString())
                        .header(HttpHeaders.AUTHORIZATION, bearer()))
                .andExpect(status().isOk())
                // Totais refletem a unidade (1 unidade no escopo).
                .andExpect(jsonPath("$.data.totais.unidades").value(1))
                // Cobertura por unidade permanece cross-unidade (todas as 7 atendidas).
                .andExpect(jsonPath("$.data.coberturaPorUnidade.length()").value(7))
                .andExpect(jsonPath("$.data.serieAgregada.serie").isArray());
    }

    @Test
    @DisplayName("Operacional com ?unidadeId= restringe a situacao a uma unica unidade")
    void operacionalFiltradoPorUnidade() throws Exception {
        UUID unidadeId = unidadeAtendida();
        mvc.perform(get("/painel/operacional").param("unidadeId", unidadeId.toString())
                        .header(HttpHeaders.AUTHORIZATION, bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.unidades.length()").value(1))
                .andExpect(jsonPath("$.data.unidades[0].unidadeId").value(unidadeId.toString()));
    }

    /** Uma unidade atendida (nao-hub) qualquer — todas tem estoque/posicoes semeadas. */
    private UUID unidadeAtendida() {
        return unidadeRepository.findAll().stream()
                .filter(u -> !u.isHub()).findFirst().orElseThrow().getId();
    }
}
