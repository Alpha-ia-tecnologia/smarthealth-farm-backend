package com.alphatech.cahosp.ingestao;

import com.alphatech.cahosp.ingestao.dominio.StatusFonte;
import com.alphatech.cahosp.ingestao.dto.FonteDadoResponse;
import com.alphatech.cahosp.ingestao.dto.QualidadeCategoriaResponse;
import com.alphatech.cahosp.ingestao.dto.ResumoIngestaoResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Regra de negocio da ingestao de dados (RF-DAD): consulta de fontes, qualidade por categoria
 * de insumo e KPIs do painel. Modulo de governanca, somente leitura.
 */
@Service
@Transactional(readOnly = true)
public class IngestaoService {

    private final FonteDadoRepository fonteRepository;
    private final QualidadeCategoriaRepository qualidadeRepository;
    private final CalculadoraIngestao calculadora;

    public IngestaoService(FonteDadoRepository fonteRepository,
                           QualidadeCategoriaRepository qualidadeRepository,
                           CalculadoraIngestao calculadora) {
        this.fonteRepository = fonteRepository;
        this.qualidadeRepository = qualidadeRepository;
        this.calculadora = calculadora;
    }

    /** Lista as fontes de dados ordenadas (RF-DAD-02/07). */
    public List<FonteDadoResponse> listarFontes() {
        return fonteRepository.findAllByOrderByOrdemAsc().stream()
                .map(FonteDadoResponse::de)
                .toList();
    }

    /** Lista a maturidade/qualidade por categoria (RF-DAD-04). */
    public List<QualidadeCategoriaResponse> listarQualidade() {
        return qualidadeRepository.findAllByOrderByCategoriaAsc().stream()
                .map(QualidadeCategoriaResponse::de)
                .toList();
    }

    /** KPIs do painel de ingestao (RF-DAD-01/02/03/04). */
    public ResumoIngestaoResponse resumo() {
        var fontes = fonteRepository.findAllByOrderByOrdemAsc();
        long registros = fontes.stream().mapToLong(f -> f.getRegistros()).sum();
        long sincronizadas = fonteRepository.countByStatus(StatusFonte.SINCRONIZADO);
        int qualidadeMedia = calculadora.qualidadeMedia(
                fontes.stream().map(f -> f.getQualidade()).toList());
        return new ResumoIngestaoResponse(
                registros,
                fontes.size(),
                sincronizadas,
                qualidadeMedia,
                true);
    }
}
