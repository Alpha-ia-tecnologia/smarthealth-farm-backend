package com.alphatech.cahosp.recomendacao;

import com.alphatech.cahosp.comum.excecao.RecursoNaoEncontradoException;
import com.alphatech.cahosp.comum.excecao.RegraNegocioException;
import com.alphatech.cahosp.insumo.InsumoRepository;
import com.alphatech.cahosp.insumo.dominio.Insumo;
import com.alphatech.cahosp.recomendacao.dominio.OrigemMotor;
import com.alphatech.cahosp.recomendacao.dominio.Prioridade;
import com.alphatech.cahosp.recomendacao.dominio.Recomendacao;
import com.alphatech.cahosp.recomendacao.dominio.StatusRecomendacao;
import com.alphatech.cahosp.recomendacao.dominio.TipoRecomendacao;
import com.alphatech.cahosp.recomendacao.dto.CriarRecomendacaoRequest;
import com.alphatech.cahosp.recomendacao.dto.EditarRecomendacaoRequest;
import com.alphatech.cahosp.recomendacao.dto.GeracaoRecomendacoesResponse;
import com.alphatech.cahosp.recomendacao.dto.RecomendacaoResponse;
import com.alphatech.cahosp.recomendacao.dto.ResumoRecomendacoesResponse;
import com.alphatech.cahosp.seguranca.auditoria.RegistradorAuditoria;
import com.alphatech.cahosp.seguranca.auditoria.dominio.CategoriaAuditoria;
import com.alphatech.cahosp.unidade.UnidadeRepository;
import com.alphatech.cahosp.unidade.dominio.Unidade;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

/**
 * Regra de negocio das recomendacoes (RF-REC): consulta com filtros, KPIs do painel, aprovacao/
 * execucao (acoes de Gestor) e disparo do motor de geracao.
 */
@Service
@Transactional(readOnly = true)
public class RecomendacaoService {

    private final RecomendacaoRepository recomendacaoRepository;
    private final GeradorRecomendacao geradorRecomendacao;
    private final CalculadoraRecomendacao calculadora;
    private final InsumoRepository insumoRepository;
    private final UnidadeRepository unidadeRepository;
    private final RegistradorAuditoria auditoria;

    public RecomendacaoService(RecomendacaoRepository recomendacaoRepository,
                               GeradorRecomendacao geradorRecomendacao,
                               CalculadoraRecomendacao calculadora,
                               InsumoRepository insumoRepository,
                               UnidadeRepository unidadeRepository,
                               RegistradorAuditoria auditoria) {
        this.recomendacaoRepository = recomendacaoRepository;
        this.geradorRecomendacao = geradorRecomendacao;
        this.calculadora = calculadora;
        this.insumoRepository = insumoRepository;
        this.unidadeRepository = unidadeRepository;
        this.auditoria = auditoria;
    }

    /**
     * Lista recomendacoes, paginada, com filtros opcionais. A ordenacao default (maior economia
     * primeiro) vem do controller. RF-REC-01.
     */
    public Page<RecomendacaoResponse> listar(TipoRecomendacao tipo, StatusRecomendacao status,
                                             OrigemMotor origemMotor, Prioridade prioridade,
                                             UUID unidadeId, UUID insumoId, String busca,
                                             Pageable pageable) {
        String termo = (busca == null || busca.isBlank()) ? null : busca.trim();
        return recomendacaoRepository
                .buscarComFiltros(tipo, status, origemMotor, prioridade, unidadeId, insumoId,
                        termo, pageable)
                .map(RecomendacaoResponse::de);
    }

    /** KPIs do painel de recomendacoes, com filtros opcionais de unidade/insumo (RF-REC-01/02/03/05). */
    public ResumoRecomendacoesResponse resumo(UUID unidadeId, UUID insumoId) {
        long total = recomendacaoRepository.contarPainel(null, null, unidadeId, insumoId);
        long pendentes = recomendacaoRepository.contarPainel(StatusRecomendacao.PENDENTE, null, unidadeId, insumoId);
        long aprovadas = recomendacaoRepository.contarPainel(StatusRecomendacao.APROVADA, null, unidadeId, insumoId);
        long executadas = recomendacaoRepository.contarPainel(StatusRecomendacao.EXECUTADA, null, unidadeId, insumoId);
        // Adesão = aproveitadas (aprovadas + executadas); recusadas não contam como adesão.
        long aderiram = aprovadas + executadas;
        long geradasPorIA = recomendacaoRepository.contarPainel(
                null, OrigemMotor.APRENDIZADO_MAQUINA, unidadeId, insumoId);
        BigDecimal economia = recomendacaoRepository.somarEconomiaEstimadaFiltrada(unidadeId, insumoId)
                .setScale(2, RoundingMode.HALF_UP);
        int taxaAdesao = total == 0 ? 0 : Math.round((aderiram * 100f) / total);
        return new ResumoRecomendacoesResponse(
                pendentes, aprovadas, executadas, economia, geradasPorIA, taxaAdesao, total);
    }

    /** Aprova uma recomendacao pendente (RF-REC — acao de Gestor). */
    @Transactional
    public RecomendacaoResponse aprovar(UUID id) {
        Recomendacao recomendacao = carregar(id);
        recomendacao.aprovar();
        RecomendacaoResponse resposta = RecomendacaoResponse.de(recomendacaoRepository.save(recomendacao));
        auditoria.registrar(CategoriaAuditoria.APROVAR_RECOMENDACAO, "recomendacao:" + id);
        return resposta;
    }

    /** Marca uma recomendacao aprovada como executada (RF-REC-05 — acao de Gestor). */
    @Transactional
    public RecomendacaoResponse executar(UUID id) {
        Recomendacao recomendacao = carregar(id);
        recomendacao.executar();
        RecomendacaoResponse resposta = RecomendacaoResponse.de(recomendacaoRepository.save(recomendacao));
        auditoria.registrar(CategoriaAuditoria.EXECUTAR_RECOMENDACAO, "recomendacao:" + id);
        return resposta;
    }

    /**
     * Cria manualmente uma transferência (redistribuição) entre unidades (RF-REC-05 — acao de
     * Gestor). Nasce {@code PENDENTE}, com origem MANUAL e economia calculada pela quantidade.
     */
    @Transactional
    public RecomendacaoResponse criar(CriarRecomendacaoRequest req) {
        if (req.unidadeOrigemId().equals(req.unidadeDestinoId())) {
            throw new RegraNegocioException("A unidade de origem deve ser diferente da de destino.");
        }
        Insumo insumo = buscarInsumo(req.insumoId());
        Unidade origem = buscarUnidade(req.unidadeOrigemId());
        Unidade destino = buscarUnidade(req.unidadeDestinoId());

        String justificativa = (req.justificativa() == null || req.justificativa().isBlank())
                ? "Transferência manual de " + origem.getSigla() + " para " + destino.getSigla() + "."
                : req.justificativa().trim();
        Prioridade prioridade = req.prioridade() == null ? Prioridade.IMPORTANTE : req.prioridade();

        Recomendacao recomendacao = new Recomendacao(
                TipoRecomendacao.REDISTRIBUICAO, insumo, destino, origem, req.quantidade(),
                justificativa, OrigemMotor.MANUAL, prioridade, calculadora.economiaManual(req.quantidade()));
        RecomendacaoResponse resposta = RecomendacaoResponse.de(recomendacaoRepository.save(recomendacao));
        auditoria.registrar(CategoriaAuditoria.CRIAR_RECOMENDACAO, "recomendacao:" + resposta.id());
        return resposta;
    }

    /**
     * Edita uma recomendação ainda pendente (RF-REC-05 — acao de Gestor): insumo, unidades e
     * quantidade. A origem segue o tipo (obrigatória em redistribuição, nula em reposição) e a
     * economia é recalculada.
     */
    @Transactional
    public RecomendacaoResponse editar(UUID id, EditarRecomendacaoRequest req) {
        Recomendacao recomendacao = carregar(id);
        Insumo insumo = buscarInsumo(req.insumoId());
        Unidade destino = buscarUnidade(req.unidadeDestinoId());

        Unidade origem = null;
        if (recomendacao.getTipo() == TipoRecomendacao.REDISTRIBUICAO) {
            if (req.unidadeOrigemId() == null) {
                throw new RegraNegocioException("A unidade de origem e obrigatoria numa redistribuicao.");
            }
            if (req.unidadeOrigemId().equals(req.unidadeDestinoId())) {
                throw new RegraNegocioException("A unidade de origem deve ser diferente da de destino.");
            }
            origem = buscarUnidade(req.unidadeOrigemId());
        }

        recomendacao.editar(insumo, destino, origem, req.quantidade(),
                calculadora.economiaManual(req.quantidade()));
        RecomendacaoResponse resposta = RecomendacaoResponse.de(recomendacaoRepository.save(recomendacao));
        auditoria.registrar(CategoriaAuditoria.EDITAR_RECOMENDACAO, "recomendacao:" + id);
        return resposta;
    }

    /** Recusa (descarta) uma recomendação pendente (RF-REC-05 — acao de Gestor). */
    @Transactional
    public RecomendacaoResponse recusar(UUID id) {
        Recomendacao recomendacao = carregar(id);
        recomendacao.recusar();
        RecomendacaoResponse resposta = RecomendacaoResponse.de(recomendacaoRepository.save(recomendacao));
        auditoria.registrar(CategoriaAuditoria.RECUSAR_RECOMENDACAO, "recomendacao:" + id);
        return resposta;
    }

    /** Dispara o motor de geracao por regra (RF-REC-01 — acao de Gestor). */
    @Transactional
    public GeracaoRecomendacoesResponse gerar() {
        return geradorRecomendacao.gerar();
    }

    private Insumo buscarInsumo(UUID id) {
        return insumoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Insumo nao encontrado: " + id + "."));
    }

    private Unidade buscarUnidade(UUID id) {
        return unidadeRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Unidade nao encontrada: " + id + "."));
    }

    private Recomendacao carregar(UUID id) {
        return recomendacaoRepository.findComRelacionamentos(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Recomendacao nao encontrada: " + id + "."));
    }
}
