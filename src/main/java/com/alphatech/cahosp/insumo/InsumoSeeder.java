package com.alphatech.cahosp.insumo;

import com.alphatech.cahosp.insumo.dominio.Criticidade;
import com.alphatech.cahosp.insumo.dominio.CategoriaInsumo;
import com.alphatech.cahosp.insumo.dominio.Insumo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Popula o catalogo de insumos/insumos no startup, idempotente — espelha fielmente
 * {@code ../smarthealth-farm/src/data/medicines.ts} (30 itens). RF-DAD-06.
 *
 * <p>Idempotencia via {@code codigo} (unico, ex.: {@code INS-001}): cada execucao verifica
 * a existencia antes de inserir, entao redeploys nao duplicam.
 */
@Component
@Order(20)
public class InsumoSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(InsumoSeeder.class);

    /**
     * Custo unitario (R$) de cada insumo do catalogo demo, na sua unidade de medida — base do valor
     * de consumo da Curva ABC (RF-EST). Spread realista (de R$ 0,15 a R$ 120,00) para a curva ter
     * itens A/B/C bem distribuidos. Backfillado nas linhas existentes de forma idempotente.
     */
    private static final Map<String, BigDecimal> CUSTO_UNITARIO = Map.ofEntries(
            Map.entry("INS-001", bd("2.50")),  Map.entry("INS-002", bd("8.00")),
            Map.entry("INS-003", bd("1.80")),  Map.entry("INS-004", bd("22.00")),
            Map.entry("INS-005", bd("0.90")),  Map.entry("INS-006", bd("0.15")),
            Map.entry("INS-007", bd("6.50")),  Map.entry("INS-008", bd("1.40")),
            Map.entry("INS-009", bd("9.00")),  Map.entry("INS-010", bd("12.00")),
            Map.entry("INS-011", bd("0.20")),  Map.entry("INS-012", bd("0.25")),
            Map.entry("INS-013", bd("0.80")),  Map.entry("INS-014", bd("14.00")),
            Map.entry("INS-015", bd("18.00")), Map.entry("INS-016", bd("3.20")),
            Map.entry("INS-017", bd("3.80")),  Map.entry("INS-018", bd("25.00")),
            Map.entry("INS-019", bd("120.00")), Map.entry("INS-020", bd("28.00")),
            Map.entry("INS-021", bd("0.45")),  Map.entry("INS-022", bd("2.10")),
            Map.entry("INS-023", bd("16.00")), Map.entry("INS-024", bd("1.10")),
            Map.entry("INS-025", bd("0.95")),  Map.entry("INS-026", bd("0.30")),
            Map.entry("INS-027", bd("7.50")),  Map.entry("INS-028", bd("0.40")),
            Map.entry("INS-029", bd("0.35")),  Map.entry("INS-030", bd("0.60")));

    private final InsumoRepository insumoRepository;

    public InsumoSeeder(InsumoRepository insumoRepository) {
        this.insumoRepository = insumoRepository;
    }

    private static BigDecimal bd(String valor) {
        return new BigDecimal(valor);
    }

    @Override
    public void run(String... args) {
        List<Insumo> catalogo = List.of(
                new Insumo("INS-001", "Amoxicilina + Clavulanato 500mg", "Comprimido",
                        CategoriaInsumo.ANTIBIOTICOS, "cp", Criticidade.ALTA, true),
                new Insumo("INS-002", "Ceftriaxona 1g", "Frasco-ampola",
                        CategoriaInsumo.ANTIBIOTICOS, "fa", Criticidade.ALTA, true),
                new Insumo("INS-003", "Azitromicina 500mg", "Comprimido",
                        CategoriaInsumo.ANTIBIOTICOS, "cp", Criticidade.MEDIA, true),
                new Insumo("INS-004", "Vancomicina 500mg", "Frasco-ampola",
                        CategoriaInsumo.ANTIBIOTICOS, "fa", Criticidade.ALTA, true),
                new Insumo("INS-005", "Dipirona 500mg/mL", "Ampola 2mL",
                        CategoriaInsumo.ANALGESICOS, "amp", Criticidade.MEDIA, true),
                new Insumo("INS-006", "Paracetamol 500mg", "Comprimido",
                        CategoriaInsumo.ANALGESICOS, "cp", Criticidade.BAIXA, true),
                new Insumo("INS-007", "Morfina 10mg/mL", "Ampola 1mL",
                        CategoriaInsumo.ANALGESICOS, "amp", Criticidade.ALTA, true),
                new Insumo("INS-008", "Tramadol 50mg/mL", "Ampola 2mL",
                        CategoriaInsumo.ANALGESICOS, "amp", Criticidade.MEDIA, true),
                new Insumo("INS-009", "Oseltamivir 75mg", "Cápsula",
                        CategoriaInsumo.ANTIVIRAIS, "cap", Criticidade.ALTA, true),
                new Insumo("INS-010", "Aciclovir 250mg", "Frasco-ampola",
                        CategoriaInsumo.ANTIVIRAIS, "fa", Criticidade.MEDIA, true),
                new Insumo("INS-011", "Enalapril 10mg", "Comprimido",
                        CategoriaInsumo.CARDIOVASCULAR, "cp", Criticidade.MEDIA, true),
                new Insumo("INS-012", "Losartana 50mg", "Comprimido",
                        CategoriaInsumo.CARDIOVASCULAR, "cp", Criticidade.MEDIA, true),
                new Insumo("INS-013", "Furosemida 10mg/mL", "Ampola 2mL",
                        CategoriaInsumo.CARDIOVASCULAR, "amp", Criticidade.ALTA, true),
                new Insumo("INS-014", "Noradrenalina 2mg/mL", "Ampola 4mL",
                        CategoriaInsumo.CARDIOVASCULAR, "amp", Criticidade.ALTA, true),
                new Insumo("INS-015", "Heparina 5000UI/mL", "Frasco 5mL",
                        CategoriaInsumo.CARDIOVASCULAR, "fr", Criticidade.ALTA, true),
                new Insumo("INS-016", "Soro Fisiológico 0,9% 500mL", "Bolsa",
                        CategoriaInsumo.SOROS_E_VACINAS, "bolsa", Criticidade.ALTA, true),
                new Insumo("INS-017", "Ringer Lactato 500mL", "Bolsa",
                        CategoriaInsumo.SOROS_E_VACINAS, "bolsa", Criticidade.MEDIA, true),
                new Insumo("INS-018", "Vacina Influenza", "Dose",
                        CategoriaInsumo.SOROS_E_VACINAS, "dose", Criticidade.MEDIA, true),
                new Insumo("INS-019", "Soro Antiofídico Polivalente", "Ampola",
                        CategoriaInsumo.SOROS_E_VACINAS, "amp", Criticidade.ALTA, true),
                new Insumo("INS-020", "Luva de Procedimento M", "Caixa 100un",
                        CategoriaInsumo.INSUMOS_MEDICOS, "cx", Criticidade.MEDIA, true),
                new Insumo("INS-021", "Seringa 10mL", "Unidade",
                        CategoriaInsumo.INSUMOS_MEDICOS, "un", Criticidade.BAIXA, true),
                new Insumo("INS-022", "Cateter Venoso 20G", "Unidade",
                        CategoriaInsumo.INSUMOS_MEDICOS, "un", Criticidade.MEDIA, true),
                new Insumo("INS-023", "Máscara Cirúrgica", "Caixa 50un",
                        CategoriaInsumo.INSUMOS_MEDICOS, "cx", Criticidade.BAIXA, true),
                new Insumo("INS-024", "Haloperidol 5mg/mL", "Ampola 1mL",
                        CategoriaInsumo.SAUDE_MENTAL, "amp", Criticidade.MEDIA, true),
                new Insumo("INS-025", "Diazepam 5mg/mL", "Ampola 2mL",
                        CategoriaInsumo.SAUDE_MENTAL, "amp", Criticidade.MEDIA, true),
                new Insumo("INS-026", "Clorpromazina 25mg", "Comprimido",
                        CategoriaInsumo.SAUDE_MENTAL, "cp", Criticidade.BAIXA, false),
                new Insumo("INS-027", "Artesunato + Mefloquina", "Comprimido",
                        CategoriaInsumo.ANTIPARASITARIOS, "cp", Criticidade.ALTA, true),
                new Insumo("INS-028", "Albendazol 400mg", "Comprimido",
                        CategoriaInsumo.ANTIPARASITARIOS, "cp", Criticidade.BAIXA, false),
                new Insumo("INS-029", "Ivermectina 6mg", "Comprimido",
                        CategoriaInsumo.ANTIPARASITARIOS, "cp", Criticidade.BAIXA, false),
                new Insumo("INS-030", "Doxiciclina 100mg", "Comprimido",
                        CategoriaInsumo.ANTIBIOTICOS, "cp", Criticidade.MEDIA, true)
        );

        Map<String, Insumo> existentes = insumoRepository.findAll().stream()
                .collect(Collectors.toMap(Insumo::getCodigo, Function.identity()));

        int inseridos = 0;
        int custosBackfill = 0;
        for (Insumo m : catalogo) {
            BigDecimal custo = CUSTO_UNITARIO.get(m.getCodigo());
            Insumo existente = existentes.get(m.getCodigo());
            if (existente == null) {
                m.setCustoUnitario(custo);
                insumoRepository.save(m);
                inseridos++;
            } else if (existente.getCustoUnitario() == null && custo != null) {
                // Backfill idempotente do custo nas linhas que vieram antes da Curva ABC.
                existente.setCustoUnitario(custo);
                insumoRepository.save(existente);
                custosBackfill++;
            }
        }
        if (inseridos > 0 || custosBackfill > 0) {
            log.info("Catalogo de insumos: {} novos inseridos, {} custos preenchidos (total: {}).",
                    inseridos, custosBackfill, insumoRepository.count());
        } else {
            log.info("Catalogo de insumos ja semeado (total: {}). Nada a fazer.",
                    insumoRepository.count());
        }
    }
}
