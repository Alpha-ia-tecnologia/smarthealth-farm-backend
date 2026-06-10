package com.alphatech.cahosp.previsao;

import com.alphatech.cahosp.comum.excecao.RecursoNaoEncontradoException;
import com.alphatech.cahosp.medicamento.dominio.Criticidade;
import com.alphatech.cahosp.previsao.dominio.Drift;
import com.alphatech.cahosp.previsao.dominio.Previsao;
import com.alphatech.cahosp.previsao.dto.PainelPrevisaoResponse;
import com.alphatech.cahosp.previsao.dto.PontoSerieResponse;
import com.alphatech.cahosp.previsao.dto.PrevisaoDetalheResponse;
import com.alphatech.cahosp.previsao.dto.PrevisaoResumoResponse;
import com.alphatech.cahosp.previsao.dto.RecalibracaoResponse;
import com.alphatech.cahosp.seguranca.auditoria.RegistradorAuditoria;
import com.alphatech.cahosp.seguranca.auditoria.dominio.CategoriaAuditoria;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Regra de negocio da previsao de demanda (RF-PRV): consulta, KPIs e recalibracao.
 */
@Service
@Transactional(readOnly = true)
public class PrevisaoService {

    private final PrevisaoRepository previsaoRepository;
    private final CalculadoraPrevisao calculadora;
    private final RegistradorAuditoria auditoria;

    public PrevisaoService(PrevisaoRepository previsaoRepository, CalculadoraPrevisao calculadora,
                           RegistradorAuditoria auditoria) {
        this.previsaoRepository = previsaoRepository;
        this.calculadora = calculadora;
        this.auditoria = auditoria;
    }

    /** Lista previsoes com filtros opcionais (unidade, medicamento, drift, busca). RF-PRV-01. */
    public List<PrevisaoResumoResponse> listar(UUID unidadeId, UUID medicamentoId,
                                               Drift drift, String busca) {
        String termo = (busca == null || busca.isBlank()) ? null : busca.trim();
        return previsaoRepository
                .buscarComFiltros(unidadeId, medicamentoId, drift, termo,
                        Sort.by("medicamento.nome").ascending())
                .stream()
                .map(PrevisaoResumoResponse::de)
                .toList();
    }

    /** Detalha uma previsao com a serie temporal completa. RF-PRV-02. */
    public PrevisaoDetalheResponse detalhar(UUID medicamentoId, UUID unidadeId) {
        Previsao previsao = previsaoRepository.findDetalhe(medicamentoId, unidadeId)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Previsao nao encontrada para o medicamento/unidade informados."));
        List<PontoSerieResponse> serie = previsao.getSerie().stream()
                .map(PontoSerieResponse::de)
                .toList();
        return new PrevisaoDetalheResponse(PrevisaoResumoResponse.de(previsao), serie);
    }

    /** KPIs do painel de previsao. RF-PRV-04/05/06. */
    public PainelPrevisaoResponse resumo() {
        List<Previsao> previsoes = previsaoRepository.findTodasComMedicamento();
        long ativas = previsoes.size();
        long totalCriticos = previsoes.stream().filter(this::ehCritico).count();
        long criticosNaMeta = previsoes.stream()
                .filter(this::ehCritico)
                .filter(p -> calculadora.dentroDaMeta(p.getMape()))
                .count();
        long comDesvio = previsoes.stream().filter(p -> p.getDrift() == Drift.DEGRADADO).count();
        BigDecimal mapeMedio = mediaMape(previsoes);
        return new PainelPrevisaoResponse(mapeMedio, criticosNaMeta, totalCriticos, ativas, comDesvio);
    }

    /** Recalibra todas as previsoes (RF-PRV — acao de Gestor). */
    @Transactional
    public RecalibracaoResponse recalibrar() {
        LocalDate hoje = LocalDate.now();
        List<Previsao> previsoes = previsaoRepository.findAll();
        previsoes.forEach(p -> p.recalibrar(hoje));
        auditoria.registrar(CategoriaAuditoria.RECALIBRAR_PREVISAO, "previsao:todas");
        return new RecalibracaoResponse(previsoes.size(), hoje,
                previsoes.size() + " previsoes recalibradas; drift estabilizado.");
    }

    private boolean ehCritico(Previsao p) {
        return p.getMedicamento().getCriticidade() == Criticidade.ALTA;
    }

    private BigDecimal mediaMape(List<Previsao> previsoes) {
        if (previsoes.isEmpty()) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal soma = previsoes.stream()
                .map(Previsao::getMape)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return soma.divide(BigDecimal.valueOf(previsoes.size()), 2, RoundingMode.HALF_UP);
    }
}
