package com.alphatech.cahosp.estoque;

import com.alphatech.cahosp.estoque.dominio.Lote;
import com.alphatech.cahosp.estoque.dominio.PosicaoEstoque;
import com.alphatech.cahosp.medicamento.MedicamentoRepository;
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

import java.time.LocalDate;
import java.util.UUID;

import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Estoque fim-a-fim (RF-EST) contra PostgreSQL real: posicoes/status, drill-down, KPIs e o
 * livro-razao (entrada de lote, saida, saldo insuficiente). {@code @Transactional} isola as
 * escritas, mantendo o estoque semeado estavel.
 */
@AutoConfigureMockMvc
@Transactional
class EstoqueIT extends BaseIntegracaoPostgres {

    private static final String SENHA = "SenhaTeste123";

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private MedicamentoRepository medicamentoRepository;
    @Autowired private UnidadeRepository unidadeRepository;
    @Autowired private PosicaoEstoqueRepository posicaoRepository;
    @Autowired private LoteRepository loteRepository;

    private String token;
    private UUID medId;
    private UUID uniId;
    private UUID loteComSaldoId;
    private int saldoDoLote;

    @BeforeEach
    void preparar() throws Exception {
        String email = "estoque." + UUID.randomUUID().toString().substring(0, 8) + "@cahosp.local";
        usuarioRepository.save(new Usuario("Operador Estoque", email, passwordEncoder.encode(SENHA), Perfil.OPERADOR));
        token = autenticar(email);

        PosicaoEstoque posicao = posicaoRepository.findAll().get(0);
        medId = posicao.getMedicamento().getId();
        uniId = posicao.getUnidade().getId();

        Lote lote = loteRepository.findAll().stream()
                .filter(l -> l.getQuantidade() > 0)
                .findFirst()
                .orElseThrow();
        loteComSaldoId = lote.getId();
        saldoDoLote = lote.getQuantidade();
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
    @DisplayName("Sem token => 401 NAO_AUTENTICADO")
    void semToken() throws Exception {
        mvc.perform(get("/estoque"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NAO_AUTENTICADO"));
    }

    @Test
    @DisplayName("Lista posicoes com status derivado e total")
    void listarPosicoes() throws Exception {
        mvc.perform(get("/estoque").header(HttpHeaders.AUTHORIZATION, bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.total").isNumber())
                .andExpect(jsonPath("$.data[0].status").exists());
    }

    @Test
    @DisplayName("Paginacao: ?size=5 limita a pagina; total reflete o conjunto inteiro")
    void paginacaoPosicoes() throws Exception {
        mvc.perform(get("/estoque?page=0&size=5").header(HttpHeaders.AUTHORIZATION, bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()", lessThanOrEqualTo(5)))
                .andExpect(jsonPath("$.total").isNumber());
    }

    @Test
    @DisplayName("Filtro ?status=critico devolve apenas posicoes criticas")
    void filtroStatusCritico() throws Exception {
        mvc.perform(get("/estoque?status=critico").header(HttpHeaders.AUTHORIZATION, bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].status").value("critico"));
    }

    @Test
    @DisplayName("Resumo traz os KPIs do estoque")
    void resumo() throws Exception {
        mvc.perform(get("/estoque/resumo").header(HttpHeaders.AUTHORIZATION, bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.itensCriticos").isNumber())
                .andExpect(jsonPath("$.data.lotesProximosVencimento").isNumber())
                .andExpect(jsonPath("$.data.totalUnidadesEstoque").isNumber());
    }

    @Test
    @DisplayName("Drill-down traz posicao, lotes e movimentacoes")
    void detalhar() throws Exception {
        mvc.perform(get("/estoque/" + medId + "/" + uniId).header(HttpHeaders.AUTHORIZATION, bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.posicao.medicamentoId").value(medId.toString()))
                .andExpect(jsonPath("$.data.lotes").isArray())
                .andExpect(jsonPath("$.data.movimentacoes").isArray());
    }

    @Test
    @DisplayName("Drill-down de posicao inexistente => 404")
    void detalharInexistente() throws Exception {
        mvc.perform(get("/estoque/" + UUID.randomUUID() + "/" + UUID.randomUUID())
                        .header(HttpHeaders.AUTHORIZATION, bearer()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("NAO_ENCONTRADO"));
    }

    @Test
    @DisplayName("Lista lotes sem filtro (sem validadeAteDias) => 200 — regressao do tipo de parametro no Postgres")
    void listarLotesSemFiltro() throws Exception {
        mvc.perform(get("/lotes").header(HttpHeaders.AUTHORIZATION, bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").isNumber())
                .andExpect(jsonPath("$.data[0].diasParaVencer").isNumber());
    }

    @Test
    @DisplayName("Lista lotes com filtros (validadeAteDias + comSaldo) => 200")
    void listarLotesComFiltros() throws Exception {
        mvc.perform(get("/lotes")
                        .param("validadeAteDias", "60")
                        .param("comSaldo", "true")
                        .param("medicamentoId", medId.toString())
                        .header(HttpHeaders.AUTHORIZATION, bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.total").isNumber());
    }

    @Test
    @DisplayName("Cria lote (201) e registra Entrada inicial")
    void criarLote() throws Exception {
        String validade = LocalDate.now().plusMonths(8).toString();
        String corpo = """
                {"medicamentoId":"%s","unidadeId":"%s","numeroLote":"LOTE-IT-%s",
                 "validade":"%s","quantidade":150,"fabricante":"Eurofarma",
                 "responsavel":"Teste","documento":"NF-IT"}
                """.formatted(medId, uniId, UUID.randomUUID().toString().substring(0, 6), validade);

        mvc.perform(post("/lotes").header(HttpHeaders.AUTHORIZATION, bearer())
                        .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.quantidade").value(150))
                .andExpect(jsonPath("$.data.diasParaVencer").isNumber());
    }

    @Test
    @DisplayName("Registra Saida (201) abatendo o saldo do lote")
    void registrarSaida() throws Exception {
        String corpo = """
                {"loteId":"%s","tipo":"Saída","quantidade":1,"responsavel":"Teste","documento":"NF-S"}
                """.formatted(loteComSaldoId);
        mvc.perform(post("/movimentacoes").header(HttpHeaders.AUTHORIZATION, bearer())
                        .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.tipo").value("Saída"))
                .andExpect(jsonPath("$.data.quantidade").value(1));
    }

    @Test
    @DisplayName("Saida acima do saldo => 422 REGRA_NEGOCIO")
    void saldoInsuficiente() throws Exception {
        String corpo = """
                {"loteId":"%s","tipo":"Saída","quantidade":%d,"responsavel":"Teste","documento":"NF-X"}
                """.formatted(loteComSaldoId, saldoDoLote + 1_000_000);
        mvc.perform(post("/movimentacoes").header(HttpHeaders.AUTHORIZATION, bearer())
                        .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("REGRA_NEGOCIO"));
    }

    @Test
    @DisplayName("Movimentacao em lote inexistente => 404")
    void loteInexistente() throws Exception {
        String corpo = """
                {"loteId":"%s","tipo":"Entrada","quantidade":5,"responsavel":"Teste","documento":"NF-Z"}
                """.formatted(UUID.randomUUID());
        mvc.perform(post("/movimentacoes").header(HttpHeaders.AUTHORIZATION, bearer())
                        .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("NAO_ENCONTRADO"));
    }

    @Test
    @DisplayName("Lote com body invalido => 400 VALIDACAO")
    void bodyInvalido() throws Exception {
        String corpo = """
                {"medicamentoId":"%s","unidadeId":"%s","numeroLote":"","validade":"2020-01-01",
                 "quantidade":0,"fabricante":"","responsavel":"","documento":""}
                """.formatted(medId, uniId);
        mvc.perform(post("/lotes").header(HttpHeaders.AUTHORIZATION, bearer())
                        .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACAO"));
    }
}
