package com.alphatech.cahosp.estoque;

import com.alphatech.cahosp.comum.excecao.RecursoNaoEncontradoException;
import com.alphatech.cahosp.estoque.dominio.Lote;
import com.alphatech.cahosp.estoque.dominio.PosicaoEstoque;
import com.alphatech.cahosp.estoque.dominio.StatusEstoque;
import com.alphatech.cahosp.estoque.dominio.TipoMovimentacao;
import com.alphatech.cahosp.estoque.dto.LoteResponse;
import com.alphatech.cahosp.estoque.dto.MovimentacaoResponse;
import com.alphatech.cahosp.estoque.dto.PosicaoEstoqueDetalheResponse;
import com.alphatech.cahosp.estoque.dto.PosicaoEstoqueResponse;
import com.alphatech.cahosp.estoque.dto.ResumoEstoqueResponse;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Leitura do estoque (RF-EST-01/02/04/05): posicoes com status derivado, drill-down por
 * lote/movimentacao e KPIs do painel.
 */
@Service
@Transactional(readOnly = true)
public class EstoqueConsultaService {

    /** Antecedencia (dias) para um lote ser considerado "proximo do vencimento". */
    private static final int DIAS_PROXIMO_VENCIMENTO = 60;

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

    /** Posicoes com filtros opcionais; o filtro de status e aplicado sobre o valor derivado. */
    public List<PosicaoEstoqueResponse> listarPosicoes(UUID unidadeId, UUID medicamentoId,
                                                       StatusEstoque status, String busca) {
        String termo = normalizar(busca);
        return posicaoRepository
                .buscarComFiltros(unidadeId, medicamentoId, termo, Sort.by("medicamento.nome").ascending())
                .stream()
                .map(this::paraResponse)
                .filter(r -> status == null || r.status() == status)
                .toList();
    }

    /** Drill-down: a posicao, seus lotes e as movimentacoes recentes. RF-EST-03/06. */
    public PosicaoEstoqueDetalheResponse detalhar(UUID medicamentoId, UUID unidadeId) {
        PosicaoEstoque posicao = posicaoRepository
                .findByMedicamentoIdAndUnidadeId(medicamentoId, unidadeId)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Posicao de estoque nao encontrada para o medicamento/unidade informados."));

        LocalDate hoje = LocalDate.now();
        List<LoteResponse> lotes = loteRepository
                .findByMedicamentoIdAndUnidadeIdOrderByValidadeAsc(medicamentoId, unidadeId)
                .stream()
                .map(l -> LoteResponse.de(l, calculadora.diasParaVencer(l.getValidade(), hoje)))
                .toList();
        List<MovimentacaoResponse> movimentacoes = movimentacaoRepository
                .findByMedicamentoIdAndUnidadeIdOrderByDataHoraDesc(medicamentoId, unidadeId)
                .stream()
                .map(MovimentacaoResponse::de)
                .toList();

        return new PosicaoEstoqueDetalheResponse(paraResponse(posicao), lotes, movimentacoes);
    }

    /** KPIs do painel de estoque (RF-EST-01/04/05). */
    public ResumoEstoqueResponse resumo() {
        List<PosicaoEstoque> posicoes = posicaoRepository.findAll();
        long itensCriticos = posicoes.stream().filter(this::critico).count();
        long totalUnidades = posicoes.stream().mapToLong(PosicaoEstoque::getQuantidade).sum();
        long leadMedio = posicoes.isEmpty() ? 0 : Math.round(posicoes.stream()
                .mapToInt(PosicaoEstoque::getTempoMedioRessuprimentoDias).average().orElse(0));
        long lotesProximos = loteRepository.countByQuantidadeGreaterThanAndValidadeLessThanEqual(
                0, LocalDate.now().plusDays(DIAS_PROXIMO_VENCIMENTO));
        return new ResumoEstoqueResponse(itensCriticos, lotesProximos, leadMedio, totalUnidades);
    }

    /** Lotes com filtros (unidade, medicamento, apenas com saldo, validade ate N dias). */
    public List<LoteResponse> listarLotes(UUID unidadeId, UUID medicamentoId,
                                          boolean apenasComSaldo, Integer validadeAteDias) {
        LocalDate hoje = LocalDate.now();
        LocalDate validadeAte = validadeAteDias == null ? null : hoje.plusDays(validadeAteDias);
        return loteRepository
                .buscarComFiltros(unidadeId, medicamentoId, apenasComSaldo, validadeAte,
                        Sort.by("validade").ascending())
                .stream()
                .map(l -> LoteResponse.de(l, calculadora.diasParaVencer(l.getValidade(), hoje)))
                .toList();
    }

    /** Movimentacoes (livro-razao) com filtros, mais recentes primeiro. RF-EST-06. */
    public List<MovimentacaoResponse> listarMovimentacoes(UUID medicamentoId, UUID unidadeId,
                                                          UUID loteId, TipoMovimentacao tipo) {
        return movimentacaoRepository
                .buscarComFiltros(medicamentoId, unidadeId, loteId, tipo,
                        Sort.by("dataHora").descending())
                .stream()
                .map(MovimentacaoResponse::de)
                .toList();
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
