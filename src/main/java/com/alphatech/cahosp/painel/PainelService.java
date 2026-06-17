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
import com.alphatech.cahosp.insumo.InsumoRepository;
import com.alphatech.cahosp.insumo.dominio.Insumo;
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
    private static final String INSUMO_FALLBACK = "INS-002";

    private final UnidadeRepository unidadeRepository;
    private final InsumoRepository insumoRepository;
    private final PosicaoEstoqueRepository posicaoRepository;
    private final LoteRepository loteRepository;
    private final AlertaRepository alertaRepository;
    private final RecomendacaoRepository recomendacaoRepository;
    private final PrevisaoRepository previsaoRepository;
    private final CalculadoraEstoque calculadoraEstoque;
    private final CalculadoraPainel calculadoraPainel;

    public PainelService(UnidadeRepository unidadeRepository,
                         InsumoRepository insumoRepository,
                         PosicaoEstoqueRepository posicaoRepository,
                         LoteRepository loteRepository,
                         AlertaRepository alertaRepository,
                         RecomendacaoRepository recomendacaoRepository,
                         PrevisaoRepository previsaoRepository,
                         CalculadoraEstoque calculadoraEstoque,
                         CalculadoraPainel calculadoraPainel) {
        this.unidadeRepository = unidadeRepository;
        this.insumoRepository = insumoRepository;
        this.posicaoRepository = posicaoRepository;
        this.loteRepository = loteRepository;
        this.alertaRepository = alertaRepository;
        this.recomendacaoRepository = recomendacaoRepository;
        this.previsaoRepository = previsaoRepository;
        this.calculadoraEstoque = calculadoraEstoque;
        this.calculadoraPainel = calculadoraPainel;
    }

    /**
     * Dashboard gerencial consolidado (RF-DASH-01). O filtro opcional {@code unidadeId} reaplica
     * em totais, serie agregada (Demanda x Previsao), alertas recentes e recomendacoes pendentes;
     * a <strong>cobertura por unidade</strong> permanece a rede inteira (visao cross-unidade).
     */
    public PainelGerencialResponse dashboard(UUID unidadeId) {
        // Cobertura por unidade e sempre a rede toda, independente do filtro.
        List<ResumoUnidadeResponse> resumos = resumosUnidades(null);
        return new PainelGerencialResponse(
                montarTotais(unidadeId, null),
                resumos.stream()
                        .map(r -> new CoberturaUnidadeResponse(r.sigla(), r.cobertura(), r.statusCobertura()))
                        .toList(),
                montarSerieAgregada(unidadeId),
                alertasRecentes(6, unidadeId, null),
                recomendacoesPendentes(4, unidadeId, null));
    }

    /**
     * Painel operacional com filas e situacao por unidade (RF-DASH-02). Filtros opcionais de
     * {@code unidadeId} e {@code insumoId} reaplicam nos totais, na fila de alertas e
     * restringem a situacao por unidade. As recomendacoes em aberto vem desfiltradas (o front
     * consome /recomendacoes com filtros proprios para esta secao).
     */
    public PainelOperacionalResponse operacional(UUID unidadeId, UUID insumoId) {
        return new PainelOperacionalResponse(
                montarTotais(unidadeId, insumoId),
                resumosUnidades(unidadeId),
                alertasRecentes(8, unidadeId, insumoId),
                recomendacoesAbertas(6));
    }

    private TotaisRedeResponse montarTotais(UUID unidadeId, UUID insumoId) {
        BigDecimal economia = recomendacaoRepository.somarEconomiaEstimadaFiltrada(unidadeId, insumoId)
                .setScale(2, RoundingMode.HALF_UP);
        LocalDate limiteVencimento = LocalDate.now().plusDays(DIAS_PROXIMO_VENCIMENTO);
        // "ativos" = nao resolvidos (ABERTO + EM_TRATAMENTO); "abertos" = somente ABERTO.
        long abertos = alertaRepository.contarPainel(null, null, StatusAlerta.ABERTO, null, unidadeId, insumoId);
        long ativos = alertaRepository.contarPainel(null, null, null, StatusAlerta.RESOLVIDO, unidadeId, insumoId);
        long insumos = (unidadeId == null && insumoId == null)
                ? insumoRepository.count()
                : posicaoRepository.contarInsumosDistintos(unidadeId, insumoId);
        long unidades = unidadeId != null ? 1 : unidadeRepository.countByHubFalse();
        return new TotaisRedeResponse(
                insumos,
                unidades,
                abertos,
                ativos,
                alertaRepository.contarPainel(TipoAlerta.DESABASTECIMENTO, null, null, StatusAlerta.RESOLVIDO, unidadeId, insumoId),
                alertaRepository.contarPainel(TipoAlerta.VENCIMENTO, null, null, StatusAlerta.RESOLVIDO, unidadeId, insumoId),
                recomendacaoRepository.contarPainel(StatusRecomendacao.PENDENTE, null, unidadeId, insumoId),
                economia,
                posicaoRepository.contarCriticosFiltrado(unidadeId, insumoId),
                loteRepository.contarProximosVencimento(limiteVencimento, unidadeId, insumoId));
    }

    /** Resumos por unidade; {@code unidadeId} opcional restringe a uma unica unidade. */
    private List<ResumoUnidadeResponse> resumosUnidades(UUID unidadeId) {
        return unidadeRepository
                .buscarComFiltros(null, null, false, true, null, Sort.by("sigla").ascending())
                .stream()
                .filter(u -> unidadeId == null || u.getId().equals(unidadeId))
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

    private SerieAgregadaResponse montarSerieAgregada(UUID unidadeId) {
        Insumo insumo = insumoMaisCritico(unidadeId);
        List<PontoSerieResponse> serie = previsaoRepository
                .agregarSeriePorInsumo(insumo.getId(), unidadeId)
                .stream()
                .map(this::paraPontoSerie)
                .toList();
        return new SerieAgregadaResponse(
                insumo.getId(),
                insumo.getCodigo(),
                insumo.getNome(),
                serie);
    }

    private Insumo insumoMaisCritico(UUID unidadeId) {
        List<Object[]> ranking = posicaoRepository.contarCriticosPorInsumo(unidadeId, PageRequest.of(0, 1));
        if (!ranking.isEmpty()) {
            UUID insumoId = (UUID) ranking.getFirst()[0];
            return insumoRepository.findById(insumoId).orElse(insumoFallback());
        }
        return insumoFallback();
    }

    private Insumo insumoFallback() {
        return insumoRepository.findByCodigoIgnoreCase(INSUMO_FALLBACK)
                .orElseGet(() -> insumoRepository.findAll(PageRequest.of(0, 1)).getContent().getFirst());
    }

    private List<AlertaResponse> alertasRecentes(int limite, UUID unidadeId, UUID insumoId) {
        return alertaRepository
                .findUrgentesNaoResolvidosFiltrado(StatusAlerta.RESOLVIDO, unidadeId, insumoId,
                        PageRequest.of(0, limite))
                .stream()
                .map(AlertaResponse::de)
                .toList();
    }

    private List<RecomendacaoResponse> recomendacoesPendentes(int limite, UUID unidadeId, UUID insumoId) {
        return recomendacaoRepository
                .findPendentesPorImpactoFiltrado(StatusRecomendacao.PENDENTE, unidadeId, insumoId,
                        PageRequest.of(0, limite))
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
