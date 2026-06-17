package com.alphatech.cahosp.ingestao;

import com.alphatech.cahosp.comum.GeradorPseudoaleatorio;
import com.alphatech.cahosp.ingestao.dominio.FonteDado;
import com.alphatech.cahosp.ingestao.dominio.GranularidadeDado;
import com.alphatech.cahosp.ingestao.dominio.QualidadeCategoria;
import com.alphatech.cahosp.ingestao.dominio.StatusFonte;
import com.alphatech.cahosp.insumo.dominio.CategoriaInsumo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

/**
 * Popula fontes de dados e qualidade por categoria no startup (idempotente), espelhando
 * {@code fontes} e {@code qualidadeCategorias} do front (src/data/index.ts). RF-DAD.
 *
 * <p>Roda apos os demais seeders ({@code @Order(80)}). Idempotente por {@code codigo} e
 * {@link CategoriaInsumo}.
 */
@Component
@Order(80)
public class IngestaoSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(IngestaoSeeder.class);

    private record DefFonte(String codigo, String nome, String geracao, StatusFonte status,
                            String ultimaIngestao, long registros, int qualidade, String procedencia) {
    }

    private static final List<DefFonte> FONTES = List.of(
            new DefFonte("f-sih", "SIH — Sistema de Internação", "Legado (2009)",
                    StatusFonte.SINCRONIZADO, "2026-06-08T03:10:00Z", 1_284_500, 82,
                    "EMSERH/DTI · export diário"),
            new DefFonte("f-farmaweb", "FarmaWeb — Dispensação", "API EMSERH v2",
                    StatusFonte.SINCRONIZADO, "2026-06-08T04:00:00Z", 962_300, 91,
                    "API REST · webhook"),
            new DefFonte("f-almox", "Almoxarifado Central", "Planilhas (CSV)",
                    StatusFonte.ATRASADO, "2026-06-06T18:30:00Z", 145_800, 64,
                    "Upload manual semanal"),
            new DefFonte("f-compras", "Sistema de Compras", "Legado (2014)",
                    StatusFonte.SINCRONIZADO, "2026-06-08T02:40:00Z", 73_400, 77,
                    "Replicação ETL noturna"),
            new DefFonte("f-epidemio", "Vigilância Epidemiológica", "API SES-MA",
                    StatusFonte.ERRO, "2026-06-05T11:00:00Z", 28_900, 70,
                    "Boletins dengue/lepto · API"),
            new DefFonte("f-cnes", "CNES — Cadastro de Unidades", "API DATASUS",
                    StatusFonte.SINCRONIZADO, "2026-06-07T22:15:00Z", 12, 96,
                    "Datasus · mensal"));

    private final FonteDadoRepository fonteRepository;
    private final QualidadeCategoriaRepository qualidadeRepository;

    public IngestaoSeeder(FonteDadoRepository fonteRepository,
                          QualidadeCategoriaRepository qualidadeRepository) {
        this.fonteRepository = fonteRepository;
        this.qualidadeRepository = qualidadeRepository;
    }

    @Override
    public void run(String... args) {
        int novasFontes = semearFontes();
        int novasQualidades = semearQualidade();
        if (novasFontes > 0 || novasQualidades > 0) {
            log.info("Ingestao semeada: {} fontes, {} qualidades por categoria (total fontes: {}).",
                    novasFontes, novasQualidades, fonteRepository.count());
        }
    }

    private int semearFontes() {
        int inseridas = 0;
        int ordem = 0;
        for (DefFonte def : FONTES) {
            if (fonteRepository.existsByCodigoIgnoreCase(def.codigo())) {
                ordem++;
                continue;
            }
            fonteRepository.save(new FonteDado(
                    def.codigo(),
                    def.nome(),
                    def.geracao(),
                    def.status(),
                    Instant.parse(def.ultimaIngestao()),
                    def.registros(),
                    def.qualidade(),
                    def.procedencia(),
                    ordem++));
            inseridas++;
        }
        return inseridas;
    }

    private int semearQualidade() {
        int inseridas = 0;
        CategoriaInsumo[] categorias = CategoriaInsumo.values();
        for (int i = 0; i < categorias.length; i++) {
            CategoriaInsumo categoria = categorias[i];
            if (qualidadeRepository.existsByCategoria(categoria)) {
                continue;
            }
            var r = GeradorPseudoaleatorio.comSemente("qf" + categoria.rotulo());
            GranularidadeDado granularidade = switch (i % 3) {
                case 0 -> GranularidadeDado.DIARIA;
                case 1 -> GranularidadeDado.SEMANAL;
                default -> GranularidadeDado.MENSAL;
            };
            qualidadeRepository.save(new QualidadeCategoria(
                    categoria,
                    55 + (int) Math.round(r.proximo() * 42),
                    60 + (int) Math.round(r.proximo() * 38),
                    58 + (int) Math.round(r.proximo() * 40),
                    granularidade,
                    (int) Math.round(r.proximo() * 14)));
            inseridas++;
        }
        return inseridas;
    }
}
