package com.alphatech.cahosp.previsao;

import com.alphatech.cahosp.comum.GeradorPseudoaleatorio;
import com.alphatech.cahosp.insumo.InsumoRepository;
import com.alphatech.cahosp.insumo.dominio.Criticidade;
import com.alphatech.cahosp.insumo.dominio.CategoriaInsumo;
import com.alphatech.cahosp.insumo.dominio.Insumo;
import com.alphatech.cahosp.previsao.dominio.Drift;
import com.alphatech.cahosp.previsao.dominio.PontoSerie;
import com.alphatech.cahosp.previsao.dominio.Previsao;
import com.alphatech.cahosp.unidade.UnidadeRepository;
import com.alphatech.cahosp.unidade.dominio.Porte;
import com.alphatech.cahosp.unidade.dominio.Unidade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Popula as previsoes de demanda no startup (idempotente), portando {@code gerarSerie} e a
 * geracao de previsoes do front (sazonalidade epidemiologica + PRNG determinístico). RF-PRV.
 *
 * <p>Roda apos catalogo/estoque ({@code @Order(40)}). Gera, por (insumo, unidade atendida),
 * 1 previsao com serie de 12 meses historicos (realizado x previsto) + 3 meses de previsao.
 */
@Component
@Order(40)
public class PrevisaoSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(PrevisaoSeeder.class);

    private static final String MODELO = "Modelo preditivo híbrido";
    private static final LocalDate CALIBRADO_EM = LocalDate.of(2026, 6, 1);
    private static final List<String> MESES = List.of(
            "2025-06", "2025-07", "2025-08", "2025-09", "2025-10", "2025-11",
            "2025-12", "2026-01", "2026-02", "2026-03", "2026-04", "2026-05");
    private static final List<String> MESES_PREVISAO = List.of("2026-06", "2026-07", "2026-08");
    private static final Map<String, Double> SAZONALIDADE = Map.ofEntries(
            Map.entry("2025-06", 0.92), Map.entry("2025-07", 0.90), Map.entry("2025-08", 0.94),
            Map.entry("2025-09", 1.00), Map.entry("2025-10", 1.05), Map.entry("2025-11", 1.12),
            Map.entry("2025-12", 1.20), Map.entry("2026-01", 1.35), Map.entry("2026-02", 1.42),
            Map.entry("2026-03", 1.38), Map.entry("2026-04", 1.22), Map.entry("2026-05", 1.05),
            Map.entry("2026-06", 0.95), Map.entry("2026-07", 0.92), Map.entry("2026-08", 0.95));

    private final InsumoRepository insumoRepository;
    private final UnidadeRepository unidadeRepository;
    private final PrevisaoRepository previsaoRepository;

    public PrevisaoSeeder(InsumoRepository insumoRepository,
                          UnidadeRepository unidadeRepository,
                          PrevisaoRepository previsaoRepository) {
        this.insumoRepository = insumoRepository;
        this.unidadeRepository = unidadeRepository;
        this.previsaoRepository = previsaoRepository;
    }

    @Override
    public void run(String... args) {
        if (previsaoRepository.count() > 0) {
            log.info("Previsoes ja semeadas ({}). Nada a fazer.", previsaoRepository.count());
            return;
        }
        List<Insumo> insumos = insumoRepository.findAll();
        List<Unidade> unidades = unidadeRepository.findAll().stream()
                .filter(u -> !u.isHub())
                .toList();

        int total = 0;
        for (Insumo med : insumos) {
            for (Unidade uni : unidades) {
                previsaoRepository.save(gerarPrevisao(med, uni));
                total++;
            }
        }
        log.info("Previsoes semeadas: {} ({} pontos de serie).", total, total * 15);
    }

    private Previsao gerarPrevisao(Insumo med, Unidade uni) {
        GeradorPseudoaleatorio rp =
                GeradorPseudoaleatorio.comSemente("prev" + med.getCodigo() + uni.getSigla());
        double mapeBase = med.getCriticidade() == Criticidade.ALTA
                ? 6 + rp.proximo() * 6 : 9 + rp.proximo() * 10;
        BigDecimal mape = BigDecimal.valueOf(Math.round(mapeBase * 10) / 10.0)
                .setScale(2, RoundingMode.HALF_UP);
        String versao = "v" + (2 + (int) Math.floor(rp.proximo() * 3)) + "."
                + (int) Math.floor(rp.proximo() * 9);
        double d1 = rp.proximo();
        Drift drift = d1 > 0.85 ? Drift.DEGRADADO : rp.proximo() > 0.7 ? Drift.ATENCAO : Drift.ESTAVEL;

        Previsao previsao = new Previsao(med, uni, 3, mape, MODELO, versao, drift, CALIBRADO_EM);
        gerarSerie(previsao, med, uni);
        return previsao;
    }

    /** Serie temporal: 12 meses de historico (realizado x previsto) + 3 de previsao futura. */
    private void gerarSerie(Previsao previsao, Insumo med, Unidade uni) {
        GeradorPseudoaleatorio r =
                GeradorPseudoaleatorio.comSemente(med.getCodigo() + uni.getSigla());
        double base = baseDemanda(med, uni);
        double tendencia = 1 + (r.proximo() - 0.4) * 0.15;

        int ordem = 0;
        int ultimoPrevisto = 0;
        for (int i = 0; i < MESES.size(); i++) {
            String m = MESES.get(i);
            double ruido = 1 + (r.proximo() - 0.5) * 0.18;
            double acc = base * Math.pow(tendencia, i / 12.0);
            int realizado = (int) Math.round(acc * SAZONALIDADE.get(m) * ruido);
            int previsto = (int) Math.round(realizado * (1 + (r.proximo() - 0.5) * 0.14));
            previsao.adicionarPonto(new PontoSerie(m, ordem++, realizado, previsto, null, null));
            ultimoPrevisto = previsto;
        }

        double prev = ultimoPrevisto;
        double prevSaz = SAZONALIDADE.get(MESES.get(MESES.size() - 1));
        for (String m : MESES_PREVISAO) {
            double saz = SAZONALIDADE.get(m);
            int previsto = (int) Math.round(prev * (saz / prevSaz) * (1 + (r.proximo() - 0.5) * 0.05));
            int limInferior = (int) Math.round(previsto * 0.88);
            int limSuperior = (int) Math.round(previsto * 1.12);
            previsao.adicionarPonto(new PontoSerie(m, ordem++, null, previsto, limInferior, limSuperior));
            prev = previsto;
            prevSaz = saz;
        }
    }

    /** Demanda mensal base por porte/categoria/criticidade (espelha o front). */
    private int baseDemanda(Insumo med, Unidade uni) {
        double porte = uni.getPorte() == Porte.GRANDE ? 1.0
                : uni.getPorte() == Porte.MEDIO ? 0.55 : 0.3;
        double base = med.getCategoria() == CategoriaInsumo.INSUMOS_MEDICOS ? 4200
                : med.getCategoria() == CategoriaInsumo.SOROS_E_VACINAS ? 1800
                : med.getCriticidade() == Criticidade.ALTA ? 900 : 1400;
        return (int) Math.round(base * porte);
    }
}
