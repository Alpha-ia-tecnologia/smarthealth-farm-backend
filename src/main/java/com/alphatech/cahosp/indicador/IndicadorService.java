package com.alphatech.cahosp.indicador;

import com.alphatech.cahosp.comum.excecao.RecursoNaoEncontradoException;
import com.alphatech.cahosp.indicador.dominio.IndicadorMeta;
import com.alphatech.cahosp.indicador.dto.IndicadorResponse;
import com.alphatech.cahosp.indicador.dto.ResumoIndicadoresResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Leitura dos indicadores de desempenho (RF-IND): lista com derivacoes (progresso/atingiu/
 * variacao), detalhe por codigo e KPIs do painel. Modulo de governanca, somente leitura.
 */
@Service
@Transactional(readOnly = true)
public class IndicadorService {

    private final IndicadorMetaRepository indicadorRepository;
    private final CalculadoraIndicador calculadora;
    private final CalculadoraIndicadorEscopo calculadoraEscopo;

    public IndicadorService(IndicadorMetaRepository indicadorRepository,
                            CalculadoraIndicador calculadora,
                            CalculadoraIndicadorEscopo calculadoraEscopo) {
        this.indicadorRepository = indicadorRepository;
        this.calculadora = calculadora;
        this.calculadoraEscopo = calculadoraEscopo;
    }

    /** Lista todos os indicadores (rede) com historico e derivacoes, na ordem. RF-IND-01..06. */
    public List<IndicadorResponse> listar() {
        return listar(null, null);
    }

    /**
     * Lista os indicadores recalculando o <strong>valor atual</strong> no escopo do filtro
     * (unidade/insumo) para os que tem dado real; {@code baseline}/{@code meta}/historico seguem do
     * edital. Sem filtro (ambos nulos) e identico a {@link #listar()}. RF-IND/RF-DASH.
     */
    public List<IndicadorResponse> listar(UUID unidadeId, UUID insumoId) {
        return indicadorRepository.buscarTodosComHistorico().stream()
                .map(i -> montar(i, unidadeId, insumoId))
                .toList();
    }

    private IndicadorResponse montar(IndicadorMeta indicador, UUID unidadeId, UUID insumoId) {
        return calculadoraEscopo.ajustar(indicador, unidadeId, insumoId)
                .map(a -> IndicadorResponse.de(
                        indicador, calculadora, a.atual(),
                        a.numeradorAbsoluto() != null ? a.numeradorAbsoluto() : indicador.getNumeradorAbsoluto(),
                        a.denominadorAbsoluto() != null ? a.denominadorAbsoluto() : indicador.getDenominadorAbsoluto()))
                .orElseGet(() -> IndicadorResponse.de(indicador, calculadora));
    }

    /** Detalha um indicador pelo codigo de negocio (ex.: {@code ind-ruptura}). */
    public IndicadorResponse detalhar(String codigo) {
        IndicadorMeta indicador = indicadorRepository.findByCodigoComHistorico(codigo)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Indicador nao encontrado: " + codigo + "."));
        return IndicadorResponse.de(indicador, calculadora);
    }

    /** KPIs do painel: total monitorado, metas atingidas e em progresso. RF-IND-04. */
    public ResumoIndicadoresResponse resumo() {
        List<IndicadorMeta> indicadores = indicadorRepository.findAll();
        long total = indicadores.size();
        long atingidas = indicadores.stream().filter(calculadora::atingiu).count();
        return new ResumoIndicadoresResponse(total, atingidas, total - atingidas);
    }
}
