package com.alphatech.cahosp.indicador;

import com.alphatech.cahosp.comum.GeradorPseudoaleatorio;
import com.alphatech.cahosp.indicador.dominio.IndicadorMeta;
import com.alphatech.cahosp.indicador.dominio.PontoHistorico;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Popula os indicadores de desempenho no startup (idempotente), espelhando os 6 indicadores do
 * front (src/data/index.ts) e portando {@code serieIndicador} (interpolacao linear da base ate o
 * valor atual, com ruido determinístico). RF-IND.
 *
 * <p>Roda apos os demais seeders ({@code @Order(70)}). Idempotente por {@code codigo}.
 */
@Component
@Order(70)
public class IndicadorSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(IndicadorSeeder.class);

    private static final List<String> MESES = List.of(
            "2025-06", "2025-07", "2025-08", "2025-09", "2025-10", "2025-11",
            "2025-12", "2026-01", "2026-02", "2026-03", "2026-04", "2026-05");

    /**
     * Definicao de cada indicador (espelha os 6 do front). {@code numAbs}/{@code denAbs}/
     * {@code unidadeAbs} dao o lastro em numeros reais por tras de uma taxa (nulo onde nao se
     * aplica). Os mesmos valores sao backfillados em bancos existentes pela migration V17.
     */
    private record Def(String codigo, String nome, String unidade, double baseline, double atual,
                       double meta, int metaReducaoPct, boolean melhorMenor,
                       Double numAbs, Double denAbs, String unidadeAbs) {
    }

    private static final List<Def> INDICADORES = List.of(
            new Def("ind-ruptura", "Taxa de desabastecimento de essenciais", "%",
                    18.4, 11.2, 11.96, 35, true, 9.0, 80.0, "itens essenciais"),
            new Def("ind-vencimento", "Perdas por vencimento", "%",
                    6.1, 4.3, 4.58, 25, true, 13.0, 302.0, "lotes"),
            new Def("ind-emergencial", "Compras emergenciais", "R$ mil",
                    1240, 812, 868, 30, true, null, null, null),
            new Def("ind-mape", "Assertividade da previsão (MAPE)", "%",
                    22.0, 11.8, 15, 0, true, null, null, null),
            new Def("ind-ressuprimento", "Tempo médio de ressuprimento", "dias",
                    19.5, 13.7, 14, 0, true, null, null, null),
            new Def("ind-rupturas-evitadas", "Desabastecimentos evitados (acum.)", "un",
                    0, 147, 120, 0, false, null, null, null));

    private final IndicadorMetaRepository indicadorRepository;

    public IndicadorSeeder(IndicadorMetaRepository indicadorRepository) {
        this.indicadorRepository = indicadorRepository;
    }

    @Override
    public void run(String... args) {
        if (indicadorRepository.count() > 0) {
            log.info("Indicadores ja semeados ({}). Nada a fazer.", indicadorRepository.count());
            return;
        }
        int ordem = 0;
        for (Def def : INDICADORES) {
            indicadorRepository.save(montar(def, ordem++));
        }
        log.info("Indicadores semeados: {} ({} pontos de historico).",
                INDICADORES.size(), INDICADORES.size() * MESES.size());
    }

    private IndicadorMeta montar(Def def, int ordem) {
        IndicadorMeta indicador = new IndicadorMeta(def.codigo(), def.nome(), def.unidade(),
                bd(def.baseline()), bd(def.atual()), bd(def.meta()),
                def.metaReducaoPct(), def.melhorMenor(), ordem);
        if (def.numAbs() != null) {
            indicador.definirAbsoluto(bd(def.numAbs()), bd(def.denAbs()), def.unidadeAbs());
        }
        gerarHistorico(indicador, def);
        return indicador;
    }

    /** Interpolacao linear baseline -> atual ao longo dos 12 meses, com ruido determinístico. */
    private void gerarHistorico(IndicadorMeta indicador, Def def) {
        GeradorPseudoaleatorio r = GeradorPseudoaleatorio.comSemente("ind" + def.codigo());
        int n = MESES.size();
        for (int i = 0; i < n; i++) {
            double t = (double) i / (n - 1);
            double valor = def.baseline() + (def.atual() - def.baseline()) * t * (0.7 + r.proximo() * 0.5);
            double arredondado = Math.round(valor * 10) / 10.0;
            indicador.adicionarPonto(new PontoHistorico(MESES.get(i), i, bd(arredondado)));
        }
    }

    private BigDecimal bd(double valor) {
        return BigDecimal.valueOf(valor).setScale(2, RoundingMode.HALF_UP);
    }
}
