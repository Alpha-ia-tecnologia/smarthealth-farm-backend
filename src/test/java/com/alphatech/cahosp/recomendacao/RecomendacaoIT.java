package com.alphatech.cahosp.recomendacao;

import com.alphatech.cahosp.medicamento.MedicamentoRepository;
import com.alphatech.cahosp.medicamento.dominio.Medicamento;
import com.alphatech.cahosp.recomendacao.dominio.Recomendacao;
import com.alphatech.cahosp.recomendacao.dominio.StatusRecomendacao;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Recomendacoes fim-a-fim (RF-REC) contra PostgreSQL real: listagem/filtros, KPIs, ciclo de
 * aprovacao/execucao (RBAC de Gestor, regra de transicao) e regeneracao pelo motor.
 * {@code @Transactional} isola as mutacoes (rollback).
 */
@AutoConfigureMockMvc
@Transactional
class RecomendacaoIT extends BaseIntegracaoPostgres {

    private static final String SENHA = "SenhaTeste123";

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private RecomendacaoRepository recomendacaoRepository;
    @Autowired private MedicamentoRepository medicamentoRepository;
    @Autowired private UnidadeRepository unidadeRepository;

    private String tokenGestor;
    private String tokenOperador;
    private UUID pendenteId;

    @BeforeEach
    void preparar() throws Exception {
        String sufixo = UUID.randomUUID().toString().substring(0, 8);
        String emailG = "gestor.rec." + sufixo + "@cahosp.local";
        String emailO = "op.rec." + sufixo + "@cahosp.local";
        usuarioRepository.save(new Usuario("Gestor Rec", emailG, passwordEncoder.encode(SENHA), Perfil.GESTOR));
        usuarioRepository.save(new Usuario("Operador Rec", emailO, passwordEncoder.encode(SENHA), Perfil.OPERADOR));
        tokenGestor = autenticar(emailG);
        tokenOperador = autenticar(emailO);

        pendenteId = recomendacaoRepository.findAll().stream()
                .filter(r -> r.getStatus() == StatusRecomendacao.PENDENTE)
                .map(Recomendacao::getId)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Seeder deveria ter recomendacoes pendentes."));
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
    @DisplayName("Sem token => 401")
    void semToken() throws Exception {
        mvc.perform(get("/recomendacoes"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NAO_AUTENTICADO"));
    }

    @Test
    @DisplayName("Lista recomendacoes com tipo, motor, prioridade, economia e status")
    void listar() throws Exception {
        mvc.perform(get("/recomendacoes").header(HttpHeaders.AUTHORIZATION, bearer(tokenOperador)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").isNumber())
                .andExpect(jsonPath("$.data[0].tipo").exists())
                .andExpect(jsonPath("$.data[0].origemMotor").exists())
                .andExpect(jsonPath("$.data[0].prioridade").exists())
                .andExpect(jsonPath("$.data[0].economiaEstimada").exists())
                .andExpect(jsonPath("$.data[0].status").exists());
    }

    @Test
    @DisplayName("Pagina no servidor: ?size=1 devolve 1 item e o total do conjunto")
    void paginar() throws Exception {
        long total = recomendacaoRepository.count();
        mvc.perform(get("/recomendacoes?page=0&size=1").header(HttpHeaders.AUTHORIZATION, bearer(tokenOperador)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.total").value((int) total));
    }

    @Test
    @DisplayName("Filtro ?tipo=Reposição (rotulo) devolve apenas reposicoes")
    void filtroTipo() throws Exception {
        mvc.perform(get("/recomendacoes?tipo=Reposição").header(HttpHeaders.AUTHORIZATION, bearer(tokenOperador)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].tipo").value("Reposição"));
    }

    @Test
    @DisplayName("Filtro ?status=Pendente devolve apenas pendentes")
    void filtroStatus() throws Exception {
        mvc.perform(get("/recomendacoes?status=Pendente").header(HttpHeaders.AUTHORIZATION, bearer(tokenOperador)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].status").value("Pendente"));
    }

    @Test
    @DisplayName("Resumo traz os KPIs de recomendacoes")
    void resumo() throws Exception {
        mvc.perform(get("/recomendacoes/resumo").header(HttpHeaders.AUTHORIZATION, bearer(tokenOperador)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pendentes").isNumber())
                .andExpect(jsonPath("$.data.economiaPotencial").isNumber())
                .andExpect(jsonPath("$.data.geradasPorIA").isNumber())
                .andExpect(jsonPath("$.data.taxaAdesao").isNumber())
                .andExpect(jsonPath("$.data.total").isNumber());
    }

    @Test
    @DisplayName("Resumo com ?unidadeId inexistente zera os KPIs (filtro aplicado)")
    void resumoFiltrado() throws Exception {
        mvc.perform(get("/recomendacoes/resumo").param("unidadeId", java.util.UUID.randomUUID().toString())
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenOperador)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(0))
                .andExpect(jsonPath("$.data.pendentes").value(0));
    }

    @Test
    @DisplayName("Gestor aprova (Pendente->Aprovada) e executa (Aprovada->Executada)")
    void aprovarEExecutar() throws Exception {
        mvc.perform(post("/recomendacoes/" + pendenteId + "/aprovar")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenGestor)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("Aprovada"));

        mvc.perform(post("/recomendacoes/" + pendenteId + "/executar")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenGestor)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("Executada"));
    }

    @Test
    @DisplayName("Executar uma recomendacao pendente (sem aprovar) => 422 REGRA_NEGOCIO")
    void executarSemAprovar() throws Exception {
        mvc.perform(post("/recomendacoes/" + pendenteId + "/executar")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenGestor)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("REGRA_NEGOCIO"));
    }

    @Test
    @DisplayName("Operador nao pode aprovar => 403 ACESSO_NEGADO")
    void aprovarComoOperador() throws Exception {
        mvc.perform(post("/recomendacoes/" + pendenteId + "/aprovar")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenOperador)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ACESSO_NEGADO"));
    }

    @Test
    @DisplayName("Aprovar recomendacao inexistente => 404 NAO_ENCONTRADO")
    void aprovarInexistente() throws Exception {
        mvc.perform(post("/recomendacoes/" + UUID.randomUUID() + "/aprovar")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenGestor)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("NAO_ENCONTRADO"));
    }

    @Test
    @DisplayName("Gestor regenera as recomendacoes (200) com KPIs da geracao")
    void gerarComoGestor() throws Exception {
        mvc.perform(post("/recomendacoes/gerar").header(HttpHeaders.AUTHORIZATION, bearer(tokenGestor)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalAtivo").isNumber())
                .andExpect(jsonPath("$.data.reposicaoGeradas").isNumber())
                .andExpect(jsonPath("$.data.redistribuicaoGeradas").isNumber());
    }

    @Test
    @DisplayName("Operador nao pode regenerar => 403 ACESSO_NEGADO")
    void gerarComoOperador() throws Exception {
        mvc.perform(post("/recomendacoes/gerar").header(HttpHeaders.AUTHORIZATION, bearer(tokenOperador)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ACESSO_NEGADO"));
    }

    private String corpoTransferencia(UUID medicamentoId, UUID origemId, UUID destinoId, int quantidade) {
        return """
                {"medicamentoId":"%s","unidadeOrigemId":"%s","unidadeDestinoId":"%s","quantidade":%d}
                """.formatted(medicamentoId, origemId, destinoId, quantidade);
    }

    @Test
    @DisplayName("Gestor cria transferencia manual (201): Redistribuição/Manual/Pendente")
    void criarTransferenciaComoGestor() throws Exception {
        Medicamento med = medicamentoRepository.findAll().get(0);
        java.util.List<Unidade> unidades = unidadeRepository.findAll();
        UUID origem = unidades.get(0).getId();
        UUID destino = unidades.get(1).getId();

        mvc.perform(post("/recomendacoes").header(HttpHeaders.AUTHORIZATION, bearer(tokenGestor))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoTransferencia(med.getId(), origem, destino, 50)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.tipo").value("Redistribuição"))
                .andExpect(jsonPath("$.data.origemMotor").value("Manual"))
                .andExpect(jsonPath("$.data.status").value("Pendente"))
                .andExpect(jsonPath("$.data.quantidade").value(50));
    }

    @Test
    @DisplayName("Criar com origem igual ao destino => 422 REGRA_NEGOCIO")
    void criarComOrigemIgualDestino() throws Exception {
        Medicamento med = medicamentoRepository.findAll().get(0);
        UUID mesma = unidadeRepository.findAll().get(0).getId();

        mvc.perform(post("/recomendacoes").header(HttpHeaders.AUTHORIZATION, bearer(tokenGestor))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoTransferencia(med.getId(), mesma, mesma, 10)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("REGRA_NEGOCIO"));
    }

    @Test
    @DisplayName("Criar com quantidade zero => 400 VALIDACAO")
    void criarComQuantidadeZero() throws Exception {
        Medicamento med = medicamentoRepository.findAll().get(0);
        java.util.List<Unidade> unidades = unidadeRepository.findAll();

        mvc.perform(post("/recomendacoes").header(HttpHeaders.AUTHORIZATION, bearer(tokenGestor))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoTransferencia(med.getId(), unidades.get(0).getId(), unidades.get(1).getId(), 0)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACAO"));
    }

    @Test
    @DisplayName("Operador nao pode criar transferencia => 403 ACESSO_NEGADO")
    void criarComoOperador() throws Exception {
        Medicamento med = medicamentoRepository.findAll().get(0);
        java.util.List<Unidade> unidades = unidadeRepository.findAll();

        mvc.perform(post("/recomendacoes").header(HttpHeaders.AUTHORIZATION, bearer(tokenOperador))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoTransferencia(med.getId(), unidades.get(0).getId(), unidades.get(1).getId(), 30)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ACESSO_NEGADO"));
    }

    @Test
    @DisplayName("Gestor edita uma transferencia recém-criada (200): nova quantidade, segue Pendente")
    void editarTransferencia() throws Exception {
        Medicamento med = medicamentoRepository.findAll().get(0);
        java.util.List<Unidade> unidades = unidadeRepository.findAll();
        UUID origem = unidades.get(0).getId();
        UUID destino = unidades.get(1).getId();

        MvcResult criada = mvc.perform(post("/recomendacoes").header(HttpHeaders.AUTHORIZATION, bearer(tokenGestor))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoTransferencia(med.getId(), origem, destino, 50)))
                .andExpect(status().isCreated())
                .andReturn();
        String novaId = objectMapper.readTree(criada.getResponse().getContentAsString())
                .path("data").path("id").asText();

        mvc.perform(put("/recomendacoes/" + novaId).header(HttpHeaders.AUTHORIZATION, bearer(tokenGestor))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoTransferencia(med.getId(), origem, destino, 80)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.quantidade").value(80))
                .andExpect(jsonPath("$.data.status").value("Pendente"));
    }

    @Test
    @DisplayName("Gestor recusa uma pendente (Pendente->Recusada); recusar de novo => 422")
    void recusarPendente() throws Exception {
        mvc.perform(post("/recomendacoes/" + pendenteId + "/recusar")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenGestor)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("Recusada"));

        mvc.perform(post("/recomendacoes/" + pendenteId + "/recusar")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenGestor)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("REGRA_NEGOCIO"));
    }
}
