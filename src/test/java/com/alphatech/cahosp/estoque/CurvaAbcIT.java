package com.alphatech.cahosp.estoque;

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

import java.util.UUID;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.in;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Curva ABC fim-a-fim (RF-EST) contra PostgreSQL real: classificacao por valor de consumo, com
 * itens e resumo por classe. Modulo somente leitura.
 */
@AutoConfigureMockMvc
@Transactional
class CurvaAbcIT extends BaseIntegracaoPostgres {

    private static final String SENHA = "SenhaTeste123";

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private UnidadeRepository unidadeRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private String token;

    @BeforeEach
    void preparar() throws Exception {
        String email = "op.abc." + UUID.randomUUID().toString().substring(0, 8) + "@cahosp.local";
        usuarioRepository.save(new Usuario("Operador ABC", email, passwordEncoder.encode(SENHA), Perfil.OPERADOR));
        MvcResult login = mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"email\": \"%s\", \"password\": \"%s\" }".formatted(email, SENHA)))
                .andExpect(status().isOk())
                .andReturn();
        token = objectMapper.readTree(login.getResponse().getContentAsString())
                .path("data").path("token").asText();
    }

    @Test
    @DisplayName("Sem token => 401")
    void semToken() throws Exception {
        mvc.perform(get("/estoque/curva-abc"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Retorna itens classificados (A/B/C) por valor de consumo e o resumo por classe")
    void curvaAbc() throws Exception {
        mvc.perform(get("/estoque/curva-abc").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.itens").isArray())
                .andExpect(jsonPath("$.data.itens.length()", greaterThan(0)))
                // ordenado por valor de consumo desc => o primeiro tem o maior acumulado relativo
                .andExpect(jsonPath("$.data.itens[0].classe", is(in(new String[] {"A", "B", "C"}))))
                .andExpect(jsonPath("$.data.itens[0].valorConsumo").exists())
                .andExpect(jsonPath("$.data.itens[0].custoUnitario").exists())
                // resumo sempre traz as tres classes
                .andExpect(jsonPath("$.data.resumo.length()").value(3))
                .andExpect(jsonPath("$.data.resumo[0].classe").value("A"));
    }

    @Test
    @DisplayName("Com ?unidadeId= restringe a Curva ABC ao consumo daquela unidade (subconjunto da rede)")
    void curvaAbcFiltradaPorUnidade() throws Exception {
        UUID unidadeId = unidadeRepository.findAll().stream()
                .filter(u -> !u.isHub()).findFirst().orElseThrow().getId();

        double totalRede = somarValorConsumo(null);
        double totalUnidade = somarValorConsumo(unidadeId);

        // O consumo de uma unica unidade nunca supera o da rede inteira, e ha consumo a classificar.
        org.assertj.core.api.Assertions.assertThat(totalUnidade).isGreaterThan(0.0);
        org.assertj.core.api.Assertions.assertThat(totalUnidade).isLessThan(totalRede);

        // Mesmo filtrada, a resposta mantem o contrato (resumo com as tres classes).
        mvc.perform(get("/estoque/curva-abc").param("unidadeId", unidadeId.toString())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.resumo.length()").value(3));
    }

    /** Soma o valorConsumo de todos os itens da Curva ABC (rede ou de uma unidade). */
    private double somarValorConsumo(UUID unidadeId) throws Exception {
        var req = get("/estoque/curva-abc").header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        if (unidadeId != null) {
            req = req.param("unidadeId", unidadeId.toString());
        }
        MvcResult res = mvc.perform(req).andExpect(status().isOk()).andReturn();
        double total = 0.0;
        for (var item : objectMapper.readTree(res.getResponse().getContentAsString())
                .path("data").path("itens")) {
            total += item.path("valorConsumo").asDouble();
        }
        return total;
    }
}
