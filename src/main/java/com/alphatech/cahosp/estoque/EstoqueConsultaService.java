package com.alphatech.cahosp.estoque;

import com.alphatech.cahosp.comum.excecao.RecursoNaoEncontradoException;
import com.alphatech.cahosp.estoque.dominio.PosicaoEstoque;
import com.alphatech.cahosp.estoque.dominio.StatusEstoque;
import com.alphatech.cahosp.estoque.dominio.TipoMovimentacao;
import com.alphatech.cahosp.estoque.dto.LoteResponse;
import com.alphatech.cahosp.estoque.dto.MovimentacaoResponse;
import com.alphatech.cahosp.estoque.dto.PosicaoEstoqueDetalheResponse;
import com.alphatech.cahosp.estoque.dto.PosicaoEstoqueResponse;
import com.alphatech.cahosp.estoque.dto.ResumoEstoqueResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Leitura do estoque (RF-EST-01/02/04/05): posicoes com status derivado, drill-down por
 * lote/movimentacao e KPIs do painel. As listagens sao paginadas no banco.
 */
@Service
@Transactional(readOnly = true)
public class EstoqueConsultaService {

    /** Antecedencia (dias) para um lote ser considerado "proximo do vencimento". */
    private static final int DIAS_PROXIMO_VENCIMENTO = 60;

    /** Movimentacoes recentes exibidas no drill-down (o livro-razao cresce sem limite). */
    private static final int MOVIMENTACOES_DETALHE = 20;

    private final PosicaoEstoqueRepository posicaoRepository;
    private final LoteRepository loteRepository;
    private final MovimentacaoRepository movimentacaoRepository;
    private final CalculadoraEstoque calculadora;

    public EstoqueConsultaService(PosicaoEstoqueRepository posicaoRepository,
                                  LoteRepository loteRepository,
                                  MovimentacaoRepository movimentacaoRepository,
                                  CalculadoraEstoque calculadora) {
        this.posicaoRepository = posicaoRepository;
        this.loteRepository = loteRepository;
        this.movimentacaoRepository = movimentacaoRepository;
        this.calculadora = calculadora;
    }

    /**
     * Posicoes paginadas com filtros opcionais. O filtro de status (derivado) e aplicado na query
     * via {@link EspecificacoesPosicao}, garantindo paginacao correta no banco.
     */
    public Page<PosicaoEstoqueResponse> listarPosicoes(UUID unidadeId, UUID insumoId,
                                                       StatusEstoque status, String busca,
                                                       Pageable pageable) {
        return posicaoRepository
                .findAll(EspecificacoesPosicao.comFiltros(unidadeId, insumoId, status, normalizar(busca)),
                        pageable)
                .map(this::paraResponse);
    }

    /** Drill-down: a posicao, seus lotes e as movimentacoes recentes. RF-EST-03/06. */
    public PosicaoEstoqueDetalheResponse detalhar(UUID insumoId, UUID unidadeId) {
        PosicaoEstoque posicao = posicaoRepository
                .findByInsumoIdAndUnidadeId(insumoId, unidadeId)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Posicao de estoque nao encontrada para o insumo/unidade informados."));

        LocalDate hoje = LocalDate.now();
        List<LoteResponse> lotes = loteRepository
                .findByInsumoIdAndUnidadeIdOrderByValidadeAsc(insumoId, unidadeId)
                .stream()
                .map(l -> LoteResponse.de(l, calculadora.diasParaVencer(l.getValidade(), hoje)))
                .toList();
        List<MovimentacaoResponse> movimentacoes = movimentacaoRepository
                .findByInsumoIdAndUnidadeIdOrderByDataHoraDesc(insumoId, unidadeId,
                        PageRequest.of(0, MOVIMENTACOES_DETALHE))
                .stream()
                .map(MovimentacaoResponse::de)
                .toList();

        return new PosicaoEstoqueDetalheResponse(paraResponse(posicao), lotes, movimentacoes);
    }

    /** KPIs do painel de estoque, com filtros opcionais de unidade/insumo (RF-EST-01/04/05). */
    public ResumoEstoqueResponse resumo(UUID unidadeId, UUID insumoId) {
        List<PosicaoEstoque> posicoes = posicaoRepository.buscarFiltrado(unidadeId, insumoId);
        long itensCriticos = posicoes.stream().filter(this::critico).count();
        long totalUnidades = posicoes.stream().mapToLong(PosicaoEstoque::getQuantidade).sum();
        long leadMedio = posicoes.isEmpty() ? 0 : Math.round(posicoes.stream()
                .mapToInt(PosicaoEstoque::getTempoMedioRessuprimentoDias).average().orElse(0));
        long lotesProximos = loteRepository.contarProximosVencimento(
                LocalDate.now().plusDays(DIAS_PROXIMO_VENCIMENTO), unidadeId, insumoId);
        return new ResumoEstoqueResponse(itensCriticos, lotesProximos, leadMedio, totalUnidades);
    }

    /** Lotes paginados com filtros (unidade, insumo, apenas com saldo, validade ate N dias). */
    public Page<LoteResponse> listarLotes(UUID unidadeId, UUID insumoId,
                                          boolean apenasComSaldo, Integer validadeAteDias,
                                          Pageable pageable) {
        LocalDate hoje = LocalDate.now();
        LocalDate validadeAte = validadeAteDias == null ? null : hoje.plusDays(validadeAteDias);
        return loteRepository
                .findAll(EspecificacoesLote.comFiltros(unidadeId, insumoId, apenasComSaldo, validadeAte),
                        pageable)
                .map(l -> LoteResponse.de(l, calculadora.diasParaVencer(l.getValidade(), hoje)));
    }

    /** Movimentacoes (livro-razao) paginadas com filtros, mais recentes primeiro. RF-EST-06. */
    public Page<MovimentacaoResponse> listarMovimentacoes(UUID insumoId, UUID unidadeId,
                                                          UUID loteId, TipoMovimentacao tipo,
                                                          Pageable pageable) {
        return movimentacaoRepository
                .buscarComFiltros(insumoId, unidadeId, loteId, tipo, pageable)
                .map(MovimentacaoResponse::de);
    }

    private PosicaoEstoqueResponse paraResponse(PosicaoEstoque p) {
        return PosicaoEstoqueResponse.de(p, calculadora.status(p.getQuantidade(), p.getNivelCritico()));
    }

    private boolean critico(PosicaoEstoque p) {
        return calculadora.status(p.getQuantidade(), p.getNivelCritico()) == StatusEstoque.CRITICO;
    }

    private String normalizar(String busca) {
        return (busca == null || busca.isBlank()) ? null : busca.trim();
    }
}
