package com.alphatech.cahosp.indicador;

import com.alphatech.cahosp.comum.excecao.RecursoNaoEncontradoException;
import com.alphatech.cahosp.indicador.dominio.IndicadorMeta;
import com.alphatech.cahosp.indicador.dto.IndicadorResponse;
import com.alphatech.cahosp.indicador.dto.ResumoIndicadoresResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Leitura dos indicadores de desempenho (RF-IND): lista com derivacoes (progresso/atingiu/
 * variacao), detalhe por codigo e KPIs do painel. Modulo de governanca, somente leitura.
 */
@Service
@Transactional(readOnly = true)
public class IndicadorService {

    private final IndicadorMetaRepository indicadorRepository;
    private final CalculadoraIndicador calculadora;

    public IndicadorService(IndicadorMetaRepository indicadorRepository,
                            CalculadoraIndicador calculadora) {
        this.indicadorRepository = indicadorRepository;
        this.calculadora = calculadora;
    }

    /** Lista todos os indicadores com historico e derivacoes, na ordem de exibicao. RF-IND-01..06. */
    public List<IndicadorResponse> listar() {
        return indicadorRepository.buscarTodosComHistorico().stream()
                .map(i -> IndicadorResponse.de(i, calculadora))
                .toList();
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
