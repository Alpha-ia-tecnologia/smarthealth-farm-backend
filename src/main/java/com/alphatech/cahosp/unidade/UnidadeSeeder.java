package com.alphatech.cahosp.unidade;

import com.alphatech.cahosp.unidade.dominio.Conectividade;
import com.alphatech.cahosp.unidade.dominio.Porte;
import com.alphatech.cahosp.unidade.dominio.Unidade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Popula o catalogo de unidades da rede EMSERH no startup, idempotente — espelha fielmente
 * {@code ../smarthealth-farm/src/data/units.ts} (8 unidades, com CAHOSP marcada como
 * {@code hub=true}). RF-DAD-06.
 *
 * <p>Idempotencia via {@code sigla} (unica): cada execucao verifica se a sigla ja existe
 * antes de inserir, entao redeploys nao duplicam dados.
 */
@Component
@Order(20)
public class UnidadeSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(UnidadeSeeder.class);

    private final UnidadeRepository unidadeRepository;

    public UnidadeSeeder(UnidadeRepository unidadeRepository) {
        this.unidadeRepository = unidadeRepository;
    }

    @Override
    public void run(String... args) {
        List<Unidade> catalogo = List.of(
                new Unidade("CAHOSP — Central de Abastecimento", "CAHOSP", "São Luís",
                        Porte.GRANDE, 0, Conectividade.ESTAVEL, "Hub logístico estadual", true),
                new Unidade("Hospital de Traumatologia e Ortopedia", "HTO", "São Luís",
                        Porte.GRANDE, 320, Conectividade.ESTAVEL, "Trauma · alta complexidade", false),
                new Unidade("Hospital Regional de Imperatriz", "HRI", "Imperatriz",
                        Porte.GRANDE, 280, Conectividade.INTERMITENTE, "Geral · referência regional", false),
                new Unidade("Hospital Macrorregional de Caxias", "HMC", "Caxias",
                        Porte.MEDIO, 160, Conectividade.INTERMITENTE, "Geral · materno-infantil", false),
                new Unidade("Hospital Regional de Balsas", "HRB", "Balsas",
                        Porte.MEDIO, 120, Conectividade.PRECARIA, "Geral · zona agrícola", false),
                new Unidade("Hospital Municipal de Açailândia", "HMA", "Açailândia",
                        Porte.MEDIO, 90, Conectividade.PRECARIA, "Geral · clínica e emergência", false),
                new Unidade("Hospital Regional de Bacabal", "HRBa", "Bacabal",
                        Porte.PEQUENO, 70, Conectividade.INTERMITENTE, "Geral · interior", false),
                new Unidade("Hospital de Chapadinha", "HCH", "Chapadinha",
                        Porte.PEQUENO, 55, Conectividade.PRECARIA, "Geral · baixa densidade", false)
        );

        int inseridas = 0;
        for (Unidade u : catalogo) {
            if (!unidadeRepository.existsBySiglaIgnoreCase(u.getSigla())) {
                unidadeRepository.save(u);
                inseridas++;
            }
        }
        if (inseridas > 0) {
            log.info("Catalogo de unidades semeado: {} novas inseridas (total: {}).",
                    inseridas, unidadeRepository.count());
        } else {
            log.info("Catalogo de unidades ja semeado (total: {}). Nada a fazer.",
                    unidadeRepository.count());
        }
    }
}
