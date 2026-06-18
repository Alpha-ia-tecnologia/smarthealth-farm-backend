package com.alphatech.cahosp.indicador;

import com.alphatech.cahosp.suporte.BaseIntegracaoPostgres;
import com.alphatech.cahosp.unidade.UnidadeRepository;
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

import java.math.BigDecimal;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Indicadores fim-a-fim (RF-IND) contra PostgreSQL real: listagem com derivacoes e historico,
 * detalhe por codigo e KPIs do painel. Modulo somente leitura.
 */
@AutoConfigureMockMvc
@Transactional
class IndicadorIT extends BaseIntegracaoPostgres {

    private static final String SENHA = "SenhaTeste123";

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private UnidadeRepository unidadeRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private String token;

    @BeforeEach
    void preparar() throws Exception {
        String email = "op.ind." + UUID.randomUUID().toString().substring(0, 8) + "@cahosp.local";
        usuarioRepository.save(new Usuario("Operador Ind", email, passwordEncoder.encode(SENHA), Perfil.OPERADOR));
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
        mvc.perform(get("/indicadores"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NAO_AUTENTICADO"));
    }

    @Test
    @DisplayName("Lista indicadores com derivacoes (progresso/atingiu) e historico")
    void listar() throws Exception {
        mvc.perform(get("/indicadores").header(HttpHeaders.AUTHORIZATION, bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(6))
                .andExpect(jsonPath("$.data[0].codigo").value("ind-ruptura"))
                .andExpect(jsonPath("$.data[0].baseline").exists())
                .andExpect(jsonPath("$.data[0].atual").exists())
                .andExpect(jsonPath("$.data[0].meta").exists())
                .andExpect(jsonPath("$.data[0].progresso").isNumber())
                .andExpect(jsonPath("$.data[0].atingiu").isBoolean())
                // Lastro em numeros reais da taxa de desabastecimento (9 de 80 itens essenciais).
                .andExpect(jsonPath("$.data[0].numeradorAbsoluto").isNumber())
                .andExpect(jsonPath("$.data[0].denominadorAbsoluto").isNumber())
                .andExpect(jsonPath("$.data[0].unidadeAbsoluta").value("itens essenciais"))
                .andExpect(jsonPath("$.data[0].historico").isArray())
                .andExpect(jsonPath("$.data[0].historico.length()").value(12));
    }

    @Test
    @DisplayName("Detalha um indicador pelo codigo, com historico de 12 meses")
    void detalhar() throws Exception {
        mvc.perform(get("/indicadores/ind-emergencial").header(HttpHeaders.AUTHORIZATION, bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.codigo").value("ind-emergencial"))
                .andExpect(jsonPath("$.data.unidade").value("R$ mil"))
                .andExpect(jsonPath("$.data.historico.length()").value(12));
    }

    @Test
    @DisplayName("Detalhe de codigo inexistente => 404 NAO_ENCONTRADO")
    void detalharInexistente() throws Exception {
        mvc.perform(get("/indicadores/ind-inexistente").header(HttpHeaders.AUTHORIZATION, bearer()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("NAO_ENCONTRADO"));
    }

    @Test
    @DisplayName("Com ?unidadeId= recalcula o valor atual no escopo, mas mantem baseline/meta do edital")
    void listarFiltradoPorUnidade() throws Exception {
        UUID unidadeId = unidadeRepository.findAll().stream()
                .filter(u -> !u.isHub()).findFirst().orElseThrow().getId();

        // Compras emergenciais (mock deterministico) na rede x no escopo: o valor deve mudar.
        BigDecimal emergencialRede = emergencialAtual(null);
        BigDecimal emergencialUnidade = emergencialAtual(unidadeId);

        mvc.perform(get("/indicadores").param("unidadeId", unidadeId.toString())
                        .header(HttpHeaders.AUTHORIZATION, bearer()))
                .andExpect(status().isOk())
                // baseline e meta sao constantes do edital (nao mudam com o filtro).
                .andExpect(jsonPath("$.data[0].codigo").value("ind-ruptura"))
                .andExpect(jsonPath("$.data[0].baseline").value(18.40))
                .andExpect(jsonPath("$.data[0].meta").value(11.96))
                // o lastro absoluto da ruptura passa a refletir os essenciais da unidade (numero real).
                .andExpect(jsonPath("$.data[0].denominadorAbsoluto").isNumber());

        // O mock de compras emergenciais varia por escopo (deixa de ser fixo).
        org.assertj.core.api.Assertions.assertThat(emergencialUnidade)
                .isNotEqualByComparingTo(emergencialRede);
    }

    /** Valor atual do indicador de compras emergenciais (rede se {@code unidadeId} nulo). */
    private BigDecimal emergencialAtual(UUID unidadeId) throws Exception {
        var req = get("/indicadores").header(HttpHeaders.AUTHORIZATION, bearer());
        if (unidadeId != null) {
            req = req.param("unidadeId", unidadeId.toString());
        }
        MvcResult res = mvc.perform(req).andExpect(status().isOk()).andReturn();
        for (var ind : objectMapper.readTree(res.getResponse().getContentAsString()).path("data")) {
            if ("ind-emergencial".equals(ind.path("codigo").asText())) {
                return new BigDecimal(ind.path("atual").asText());
            }
        }
        throw new IllegalStateException("ind-emergencial nao encontrado");
    }

    @Test
    @DisplayName("Resumo traz total, atingidas e em progresso")
    void resumo() throws Exception {
        mvc.perform(get("/indicadores/resumo").header(HttpHeaders.AUTHORIZATION, bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(6))
                .andExpect(jsonPath("$.data.atingidas").isNumber())
                .andExpect(jsonPath("$.data.emProgresso").isNumber());
    }
}
