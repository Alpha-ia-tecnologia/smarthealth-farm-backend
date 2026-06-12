package com.alphatech.cahosp.painel;

import com.alphatech.cahosp.alerta.AlertaRepository;
import com.alphatech.cahosp.alerta.dominio.StatusAlerta;
import com.alphatech.cahosp.alerta.dominio.TipoAlerta;
import com.alphatech.cahosp.alerta.dto.AlertaResponse;
import com.alphatech.cahosp.estoque.CalculadoraEstoque;
import com.alphatech.cahosp.estoque.LoteRepository;
import com.alphatech.cahosp.estoque.PosicaoEstoqueRepository;
import com.alphatech.cahosp.estoque.dominio.PosicaoEstoque;
import com.alphatech.cahosp.estoque.dominio.StatusEstoque;
import com.alphatech.cahosp.medicamento.MedicamentoRepository;
import com.alphatech.cahosp.medicamento.dominio.Medicamento;
import com.alphatech.cahosp.painel.dto.CoberturaUnidadeResponse;
import com.alphatech.cahosp.painel.dto.PainelGerencialResponse;
import com.alphatech.cahosp.painel.dto.PainelOperacionalResponse;
import com.alphatech.cahosp.painel.dto.ResumoUnidadeResponse;
import com.alphatech.cahosp.painel.dto.SerieAgregadaResponse;
import com.alphatech.cahosp.painel.dto.TotaisRedeResponse;
import com.alphatech.cahosp.previsao.PrevisaoRepository;
import com.alphatech.cahosp.previsao.SeriePeriodoAgregada;
import com.alphatech.cahosp.previsao.dto.PontoSerieResponse;
import com.alphatech.cahosp.recomendacao.RecomendacaoRepository;
import com.alphatech.cahosp.recomendacao.dominio.StatusRecomendacao;
import com.alphatech.cahosp.recomendacao.dto.RecomendacaoResponse;
import com.alphatech.cahosp.unidade.UnidadeRepository;
import com.alphatech.cahosp.unidade.dominio.Unidade;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Agregacoes de dashboard (RF-DASH-01/02): consolida totais da rede, cobertura por unidade,
 * serie agregada de previsao, filas de alertas e recomendacoes. Modulo somente leitura.
 */
@Service
@Transactional(readOnly = true)
public class PainelService {

    private static final int DIAS_PROXIMO_VENCIMENTO = 60;
    private static final String MEDICAMENTO_FALLBACK = "MED-002";

    private final UnidadeRepository unidadeRepository;
    private final MedicamentoRepository medicamentoRepository;
    private final PosicaoEstoqueRepository posicaoRepository;
    private final LoteRepository loteRepository;
    private final AlertaRepository alertaRepository;
    private final RecomendacaoRepository recomendacaoRepository;
    private final PrevisaoRepository previsaoRepository;
    private final CalculadoraEstoque calculadoraEstoque;
    private final CalculadoraPainel calculadoraPainel;

    public PainelService(UnidadeRepository unidadeRepository,
                         MedicamentoRepository medicamentoRepository,
                         PosicaoEstoqueRepository posicaoRepository,
                         LoteRepository loteRepository,
                         AlertaRepository alertaRepository,
                         RecomendacaoRepository recomendacaoRepository,
                         PrevisaoRepository previsaoRepository,
                         CalculadoraEstoque calculadoraEstoque,
                         CalculadoraPainel calculadoraPainel) {
        this.unidadeRepository = unidadeRepository;
        this.medicamentoRepository = medicamentoRepository;
        this.posicaoRepository = posicaoRepository;
        this.loteRepository = loteRepository;
        this.alertaRepository = alertaRepository;
        this.recomendacaoRepository = recomendacaoRepository;
        this.previsaoRepository = previsaoRepository;
        this.calculadoraEstoque = calculadoraEstoque;
        this.calculadoraPainel = calculadoraPainel;
    }

    /** Dashboard gerencial consolidado (RF-DASH-01). */
    public PainelGerencialResponse dashboard() {
        List<ResumoUnidadeResponse> resumos = resumosUnidades();
        return new PainelGerencialResponse(
                montarTotais(),
                resumos.stream()
                        .map(r -> new CoberturaUnidadeResponse(r.sigla(), r.cobertura(), r.statusCobertura()))
                        .toList(),
                montarSerieAgregada(),
                alertasRecentes(6),
                recomendacoesPendentes(4));
    }

    /** Painel operacional com filas e situacao por unidade (RF-DASH-02). */
    public PainelOperacionalResponse operacional() {
        return new PainelOperacionalResponse(
                montarTotais(),
                resumosUnidades(),
                alertasRecentes(8),
                recomendacoesAbertas(6));
    }

    private TotaisRedeResponse montarTotais() {
        BigDecimal economia = recomendacaoRepository.somarEconomiaEstimada()
                .setScale(2, RoundingMode.HALF_UP);
        LocalDate limiteVencimento = LocalDate.now().plusDays(DIAS_PROXIMO_VENCIMENTO);
        long abertos = alertaRepository.countByStatus(StatusAlerta.ABERTO);
        long emTratamento = alertaRepository.countByStatus(StatusAlerta.EM_TRATAMENTO);
        return new TotaisRedeResponse(
                medicamentoRepository.count(),
                unidadeRepository.countByHubFalse(),
                abertos,
                abertos + emTratamento,
                alertaRepository.countByTipoAndStatusNot(TipoAlerta.DESABASTECIMENTO, StatusAlerta.RESOLVIDO),
                alertaRepository.countByTipoAndStatusNot(TipoAlerta.VENCIMENTO, StatusAlerta.RESOLVIDO),
                recomendacaoRepository.countByStatus(StatusRecomendacao.PENDENTE),
                economia,
                posicaoRepository.contarCriticos(),
                loteRepository.countByQuantidadeGreaterThanAndValidadeLessThanEqual(0, limiteVencimento));
    }

    private List<ResumoUnidadeResponse> resumosUnidades() {
        return unidadeRepository
                .buscarComFiltros(null, null, false, true, null, Sort.by("sigla").ascending())
                .stream()
                .map(this::resumoUnidade)
                .toList();
    }

    private ResumoUnidadeResponse resumoUnidade(Unidade unidade) {
        List<PosicaoEstoque> posicoes = posicaoRepository.findByUnidadeId(unidade.getId());
        int criticos = 0;
        int atencao = 0;
        int ok = 0;
        for (PosicaoEstoque posicao : posicoes) {
            StatusEstoque status = calculadoraEstoque.status(posicao.getQuantidade(), posicao.getNivelCritico());
            if (status == StatusEstoque.CRITICO) {
                criticos++;
            } else if (status == StatusEstoque.ATENCAO) {
                atencao++;
            } else {
                ok++;
            }
        }
        int cobertura = calculadoraPainel.coberturaPercentual(ok, posicoes.size());
        long alertasAtivos = alertaRepository.countByUnidadeIdAndStatusNot(
                unidade.getId(), StatusAlerta.RESOLVIDO);
        return new ResumoUnidadeResponse(
                unidade.getId(),
                unidade.getSigla(),
                unidade.getNome(),
                unidade.getMunicipio(),
                unidade.getConectividade(),
                posicoes.size(),
                criticos,
                atencao,
                (int) alertasAtivos,
                cobertura,
                calculadoraPainel.statusCobertura(cobertura),
                calculadoraPainel.statusUnidade(criticos));
    }

    private SerieAgregadaResponse montarSerieAgregada() {
        Medicamento medicamento = medicamentoMaisCritico();
        List<PontoSerieResponse> serie = previsaoRepository
                .agregarSeriePorMedicamento(medicamento.getId())
                .stream()
                .map(this::paraPontoSerie)
                .toList();
        return new SerieAgregadaResponse(
                medicamento.getId(),
                medicamento.getCodigo(),
                medicamento.getNome(),
                serie);
    }

    private Medicamento medicamentoMaisCritico() {
        List<Object[]> ranking = posicaoRepository.contarCriticosPorMedicamento(PageRequest.of(0, 1));
        if (!ranking.isEmpty()) {
            UUID medicamentoId = (UUID) ranking.getFirst()[0];
            return medicamentoRepository.findById(medicamentoId).orElse(medicamentoFallback());
        }
        return medicamentoFallback();
    }

    private Medicamento medicamentoFallback() {
        return medicamentoRepository.findByCodigoIgnoreCase(MEDICAMENTO_FALLBACK)
                .orElseGet(() -> medicamentoRepository.findAll(PageRequest.of(0, 1)).getContent().getFirst());
    }

    private List<AlertaResponse> alertasRecentes(int limite) {
        return alertaRepository
                .findUrgentesNaoResolvidos(StatusAlerta.RESOLVIDO, PageRequest.of(0, limite))
                .stream()
                .map(AlertaResponse::de)
                .toList();
    }

    private List<RecomendacaoResponse> recomendacoesPendentes(int limite) {
        return recomendacaoRepository
                .findPendentesPorImpacto(StatusRecomendacao.PENDENTE, PageRequest.of(0, limite))
                .stream()
                .map(RecomendacaoResponse::de)
                .toList();
    }

    private List<RecomendacaoResponse> recomendacoesAbertas(int limite) {
        return recomendacaoRepository
                .findAbertas(StatusRecomendacao.EXECUTADA, PageRequest.of(0, limite))
                .stream()
                .map(RecomendacaoResponse::de)
                .toList();
    }

    private PontoSerieResponse paraPontoSerie(SeriePeriodoAgregada ponto) {
        return new PontoSerieResponse(
                ponto.getPeriodo(),
                zeroParaNull(ponto.getRealizado()),
                zeroParaNull(ponto.getPrevisto()),
                zeroParaNull(ponto.getLimiteInferior()),
                zeroParaNull(ponto.getLimiteSuperior()));
    }

    /** Espelha o {@code v.realizado || null} do front na agregacao da serie. */
    private Integer zeroParaNull(Long valor) {
        if (valor == null || valor == 0L) {
            return null;
        }
        return valor.intValue();
    }
}
