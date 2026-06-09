package com.alphatech.cahosp.alerta;

import com.alphatech.cahosp.alerta.dominio.Alerta;
import com.alphatech.cahosp.alerta.dominio.StatusAlerta;
import com.alphatech.cahosp.alerta.dto.GeracaoAlertasResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * Popula os alertas no startup (idempotente), acionando o {@link GeradorAlerta} sobre o estoque
 * e os lotes ja semeados. RF-ALE.
 *
 * <p>Roda apos estoque ({@code @Order(30)}) e previsao ({@code @Order(40)}), com {@code @Order(50)}.
 * Usa a data de referencia do mock do front ({@value #HOJE_ISO}) para reproduzir os prazos.
 * Depois marca um subconjunto deterministico como <em>Em tratamento</em>/<em>Resolvido</em>, para
 * que os KPIs ("tratados", "em tratamento") e o historico nasçam realistas.
 */
@Component
@Order(50)
public class AlertaSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AlertaSeeder.class);

    /** Mesma referencia ("hoje") do mock do front (src/data/index.ts: {@code HOJE}). */
    private static final String HOJE_ISO = "2026-06-08";
    private static final LocalDate HOJE = LocalDate.parse(HOJE_ISO);

    private final GeradorAlerta geradorAlerta;
    private final AlertaRepository alertaRepository;

    public AlertaSeeder(GeradorAlerta geradorAlerta, AlertaRepository alertaRepository) {
        this.geradorAlerta = geradorAlerta;
        this.alertaRepository = alertaRepository;
    }

    @Override
    public void run(String... args) {
        if (alertaRepository.count() > 0) {
            log.info("Alertas ja semeados ({}). Nada a fazer.", alertaRepository.count());
            return;
        }
        GeracaoAlertasResponse resultado = geradorAlerta.gerar(HOJE);
        marcarTratamentoDemo();
        log.info("Alertas semeados: {} desabastecimento, {} vencimento (total {}).",
                resultado.desabastecimentoGerados(), resultado.vencimentoGerados(),
                alertaRepository.count());
    }

    /** Marca, de forma deterministica, parte dos alertas como em tratamento/resolvido (demo). */
    private void marcarTratamentoDemo() {
        List<Alerta> alertas = alertaRepository.findAll(Sort.by("id").ascending());
        int i = 0;
        for (Alerta alerta : alertas) {
            if (i % 5 == 0) {
                alerta.mudarStatusPara(StatusAlerta.EM_TRATAMENTO);
            } else if (i % 7 == 0) {
                alerta.mudarStatusPara(StatusAlerta.RESOLVIDO);
            }
            i++;
        }
        alertaRepository.saveAll(alertas);
    }
}
