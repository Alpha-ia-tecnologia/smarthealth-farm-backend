package com.alphatech.cahosp.recomendacao;

import com.alphatech.cahosp.recomendacao.dominio.Recomendacao;
import com.alphatech.cahosp.recomendacao.dto.GeracaoRecomendacoesResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Popula as recomendacoes no startup (idempotente), acionando o {@link GeradorRecomendacao} sobre
 * o estoque ja semeado. RF-REC.
 *
 * <p>Roda apos estoque/previsao/alerta, com {@code @Order(60)}. Depois marca um subconjunto
 * deterministico como <em>Aprovada</em>/<em>Executada</em>, para que os KPIs ("taxa de adesão",
 * "pendentes") e o painel de desempenho nasçam realistas.
 */
@Component
@Order(60)
public class RecomendacaoSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(RecomendacaoSeeder.class);

    private final GeradorRecomendacao geradorRecomendacao;
    private final RecomendacaoRepository recomendacaoRepository;

    public RecomendacaoSeeder(GeradorRecomendacao geradorRecomendacao,
                              RecomendacaoRepository recomendacaoRepository) {
        this.geradorRecomendacao = geradorRecomendacao;
        this.recomendacaoRepository = recomendacaoRepository;
    }

    @Override
    public void run(String... args) {
        if (recomendacaoRepository.count() > 0) {
            log.info("Recomendacoes ja semeadas ({}). Nada a fazer.", recomendacaoRepository.count());
            return;
        }
        GeracaoRecomendacoesResponse resultado = geradorRecomendacao.gerar();
        marcarAdesaoDemo();
        log.info("Recomendacoes semeadas: {} reposicao, {} redistribuicao (total {}).",
                resultado.reposicaoGeradas(), resultado.redistribuicaoGeradas(),
                recomendacaoRepository.count());
    }

    /** Marca, de forma deterministica, parte das recomendacoes como aprovada/executada (demo). */
    private void marcarAdesaoDemo() {
        List<Recomendacao> recomendacoes = recomendacaoRepository.findAll(Sort.by("id").ascending());
        int i = 0;
        for (Recomendacao r : recomendacoes) {
            if (i % 7 == 0) {
                r.aprovar();
                r.executar();
            } else if (i % 4 == 0) {
                r.aprovar();
            }
            i++;
        }
        recomendacaoRepository.saveAll(recomendacoes);
    }
}
