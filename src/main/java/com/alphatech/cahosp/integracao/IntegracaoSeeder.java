package com.alphatech.cahosp.integracao;

import com.alphatech.cahosp.integracao.dominio.IntegracaoApi;
import com.alphatech.cahosp.integracao.dominio.ModoIntegracao;
import com.alphatech.cahosp.integracao.dominio.PapelIa;
import com.alphatech.cahosp.integracao.dominio.ProvedorIa;
import com.alphatech.cahosp.integracao.dominio.StatusIntegracao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Popula as integracoes EMSERH e os provedores de IA no startup (idempotente), espelhando
 * {@code integracoes} e {@code provedoresIA} do front (src/data/index.ts). RF-INT.
 *
 * <p>Roda apos os demais seeders ({@code @Order(90)}). Idempotente por {@code codigo}. Sao dados
 * de demonstracao do painel; a integracao real e ligada por {@code INTEGRACAO_ENABLED} (stub por
 * padrao) — fora do escopo deste modulo de visualizacao.
 */
@Component
@Order(90)
public class IntegracaoSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(IntegracaoSeeder.class);

    private record DefApi(String codigo, String nome, String versao, StatusIntegracao status,
                          int latenciaMs, String ultimaSync, ModoIntegracao modo, int registrosBuffer) {
    }

    private record DefProvedor(String codigo, String nome, boolean ativo, PapelIa papel,
                               String custoPor1kTokens, long chamadasMes, boolean anonimizacao) {
    }

    private static final List<DefApi> INTEGRACOES = List.of(
            new DefApi("api-farmaweb", "FarmaWeb API", "v2.3", StatusIntegracao.OPERACIONAL,
                    142, "2026-06-08T04:00:00Z", ModoIntegracao.ONLINE, 0),
            new DefApi("api-sih", "SIH Gateway", "v1.0", StatusIntegracao.DEGRADADA,
                    1840, "2026-06-08T03:10:00Z", ModoIntegracao.ONLINE, 0),
            new DefApi("api-balsas", "Edge · HRB Balsas", "v2.3", StatusIntegracao.INDISPONIVEL,
                    0, "2026-06-07T19:22:00Z", ModoIntegracao.OFFLINE_BUFFER, 2140),
            new DefApi("api-chapadinha", "Edge · HCH Chapadinha", "v2.3", StatusIntegracao.OPERACIONAL,
                    410, "2026-06-08T01:05:00Z", ModoIntegracao.RECONCILIANDO, 318),
            new DefApi("api-compras", "Compras EMSERH", "v1.4", StatusIntegracao.OPERACIONAL,
                    220, "2026-06-08T02:40:00Z", ModoIntegracao.ONLINE, 0));

    private static final List<DefProvedor> PROVEDORES = List.of(
            new DefProvedor("ia-deepseek", "DeepSeek", true, PapelIa.PRIMARIO, "0.0014", 48_200, true),
            new DefProvedor("ia-openai", "OpenAI", true, PapelIa.FALLBACK, "0.0050", 9_100, true),
            new DefProvedor("ia-gemini", "Google Gemini", false, PapelIa.STANDBY, "0.0035", 0, true));

    private final IntegracaoApiRepository integracaoRepository;
    private final ProvedorIaRepository provedorRepository;

    public IntegracaoSeeder(IntegracaoApiRepository integracaoRepository,
                            ProvedorIaRepository provedorRepository) {
        this.integracaoRepository = integracaoRepository;
        this.provedorRepository = provedorRepository;
    }

    @Override
    public void run(String... args) {
        int novasApis = semearIntegracoes();
        int novosProvedores = semearProvedores();
        if (novasApis > 0 || novosProvedores > 0) {
            log.info("Integracao semeada: {} integracoes, {} provedores de IA.",
                    novasApis, novosProvedores);
        }
    }

    private int semearIntegracoes() {
        int inseridas = 0;
        int ordem = 0;
        for (DefApi def : INTEGRACOES) {
            if (integracaoRepository.existsByCodigoIgnoreCase(def.codigo())) {
                ordem++;
                continue;
            }
            integracaoRepository.save(new IntegracaoApi(
                    def.codigo(), def.nome(), def.versao(), def.status(), def.latenciaMs(),
                    Instant.parse(def.ultimaSync()), def.modo(), def.registrosBuffer(), ordem++));
            inseridas++;
        }
        return inseridas;
    }

    private int semearProvedores() {
        int inseridos = 0;
        int ordem = 0;
        for (DefProvedor def : PROVEDORES) {
            if (provedorRepository.existsByCodigoIgnoreCase(def.codigo())) {
                ordem++;
                continue;
            }
            provedorRepository.save(new ProvedorIa(
                    def.codigo(), def.nome(), def.ativo(), def.papel(),
                    new BigDecimal(def.custoPor1kTokens()), def.chamadasMes(), def.anonimizacao(), ordem++));
            inseridos++;
        }
        return inseridos;
    }
}
