package com.alphatech.cahosp.estoque;

import com.alphatech.cahosp.comum.excecao.RecursoNaoEncontradoException;
import com.alphatech.cahosp.comum.excecao.RegraNegocioException;
import com.alphatech.cahosp.estoque.dominio.Lote;
import com.alphatech.cahosp.estoque.dominio.Movimentacao;
import com.alphatech.cahosp.estoque.dominio.PosicaoEstoque;
import com.alphatech.cahosp.estoque.dominio.TipoMovimentacao;
import com.alphatech.cahosp.estoque.dto.CriarLoteRequest;
import com.alphatech.cahosp.estoque.dto.LoteResponse;
import com.alphatech.cahosp.estoque.dto.MovimentacaoResponse;
import com.alphatech.cahosp.estoque.dto.RegistrarMovimentacaoRequest;
import com.alphatech.cahosp.medicamento.MedicamentoRepository;
import com.alphatech.cahosp.medicamento.dominio.Medicamento;
import com.alphatech.cahosp.unidade.UnidadeRepository;
import com.alphatech.cahosp.unidade.dominio.Unidade;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Escrita do estoque (RF-EST-03/06): entrada de lotes e lancamentos no livro-razao. Mantem
 * coerentes, na mesma transacao, o saldo do lote e a projecao {@code quantidade} da posicao.
 *
 * <p>Efeito por tipo: ENTRADA soma; SAIDA/TRANSFERENCIA subtraem (saldo nao pode ficar negativo);
 * AJUSTE define o saldo contado (recontagem de inventario).
 */
@Service
public class EstoqueMovimentacaoService {

    private final LoteRepository loteRepository;
    private final MovimentacaoRepository movimentacaoRepository;
    private final PosicaoEstoqueRepository posicaoEstoqueRepository;
    private final MedicamentoRepository medicamentoRepository;
    private final UnidadeRepository unidadeRepository;
    private final CalculadoraEstoque calculadora;

    public EstoqueMovimentacaoService(LoteRepository loteRepository,
                                      MovimentacaoRepository movimentacaoRepository,
                                      PosicaoEstoqueRepository posicaoEstoqueRepository,
                                      MedicamentoRepository medicamentoRepository,
                                      UnidadeRepository unidadeRepository,
                                      CalculadoraEstoque calculadora) {
        this.loteRepository = loteRepository;
        this.movimentacaoRepository = movimentacaoRepository;
        this.posicaoEstoqueRepository = posicaoEstoqueRepository;
        this.medicamentoRepository = medicamentoRepository;
        this.unidadeRepository = unidadeRepository;
        this.calculadora = calculadora;
    }

    /** Cria um lote e registra a Entrada inicial no livro-razao. RF-EST-03. */
    @Transactional
    public LoteResponse criarLote(CriarLoteRequest req) {
        Medicamento medicamento = medicamentoRepository.findById(req.medicamentoId())
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Medicamento nao encontrado: " + req.medicamentoId() + "."));
        Unidade unidade = unidadeRepository.findById(req.unidadeId())
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Unidade nao encontrada: " + req.unidadeId() + "."));

        Lote lote = loteRepository.save(new Lote(medicamento, unidade, req.numeroLote(),
                req.validade(), req.quantidade(), req.fabricante()));

        registrarLancamento(lote, TipoMovimentacao.ENTRADA, req.quantidade(),
                req.responsavel(), req.documento());
        posicao(medicamento, unidade).aplicarDelta(req.quantidade());

        return LoteResponse.de(lote, calculadora.diasParaVencer(lote.getValidade()));
    }

    /** Registra um lancamento sobre um lote existente, ajustando saldo e posicao. RF-EST-06. */
    @Transactional
    public MovimentacaoResponse registrar(RegistrarMovimentacaoRequest req) {
        Lote lote = loteRepository.findById(req.loteId())
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Lote nao encontrado: " + req.loteId() + "."));

        int delta = aplicarNoLote(lote, req.tipo(), req.quantidade());
        Movimentacao mov = registrarLancamento(lote, req.tipo(), req.quantidade(),
                req.responsavel(), req.documento());
        posicao(lote.getMedicamento(), lote.getUnidade()).aplicarDelta(delta);

        return MovimentacaoResponse.de(mov);
    }

    /** Aplica o efeito do tipo no saldo do lote e devolve o delta (para atualizar a posicao). */
    private int aplicarNoLote(Lote lote, TipoMovimentacao tipo, int quantidade) {
        return switch (tipo) {
            case ENTRADA -> {
                exigirPositiva(tipo, quantidade);
                lote.adicionar(quantidade);
                yield quantidade;
            }
            case SAIDA, TRANSFERENCIA -> {
                exigirPositiva(tipo, quantidade);
                if (lote.getQuantidade() < quantidade) {
                    throw new RegraNegocioException(
                            "Saldo insuficiente no lote " + lote.getNumeroLote()
                                    + " (disponivel: " + lote.getQuantidade() + ").");
                }
                lote.subtrair(quantidade);
                yield -quantidade;
            }
            case AJUSTE -> {
                int delta = quantidade - lote.getQuantidade();
                lote.ajustarPara(quantidade);
                yield delta;
            }
        };
    }

    private void exigirPositiva(TipoMovimentacao tipo, int quantidade) {
        if (quantidade <= 0) {
            throw new RegraNegocioException(
                    "A quantidade deve ser positiva para movimentacoes do tipo " + tipo.rotulo() + ".");
        }
    }

    private Movimentacao registrarLancamento(Lote lote, TipoMovimentacao tipo, int quantidade,
                                             String responsavel, String documento) {
        return movimentacaoRepository.save(
                new Movimentacao(lote, tipo, quantidade, Instant.now(), responsavel, documento));
    }

    /** Posicao da combinacao medicamento/unidade; cria uma zerada se ainda nao existir. */
    private PosicaoEstoque posicao(Medicamento medicamento, Unidade unidade) {
        return posicaoEstoqueRepository
                .findByMedicamentoIdAndUnidadeId(medicamento.getId(), unidade.getId())
                .orElseGet(() -> posicaoEstoqueRepository.save(
                        new PosicaoEstoque(medicamento, unidade, 0, 0, 0, 0, 0)));
    }
}
