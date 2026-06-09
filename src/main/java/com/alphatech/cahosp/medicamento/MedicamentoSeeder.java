package com.alphatech.cahosp.medicamento;

import com.alphatech.cahosp.medicamento.dominio.Criticidade;
import com.alphatech.cahosp.medicamento.dominio.FamiliaTerapeutica;
import com.alphatech.cahosp.medicamento.dominio.Medicamento;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Popula o catalogo de medicamentos/insumos no startup, idempotente — espelha fielmente
 * {@code ../smarthealth-farm/src/data/medicines.ts} (30 itens). RF-DAD-06.
 *
 * <p>Idempotencia via {@code codigo} (unico, ex.: {@code MED-001}): cada execucao verifica
 * a existencia antes de inserir, entao redeploys nao duplicam.
 */
@Component
@Order(20)
public class MedicamentoSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(MedicamentoSeeder.class);

    private final MedicamentoRepository medicamentoRepository;

    public MedicamentoSeeder(MedicamentoRepository medicamentoRepository) {
        this.medicamentoRepository = medicamentoRepository;
    }

    @Override
    public void run(String... args) {
        List<Medicamento> catalogo = List.of(
                new Medicamento("MED-001", "Amoxicilina + Clavulanato 500mg", "Comprimido",
                        FamiliaTerapeutica.ANTIBIOTICOS, "cp", Criticidade.ALTA, true),
                new Medicamento("MED-002", "Ceftriaxona 1g", "Frasco-ampola",
                        FamiliaTerapeutica.ANTIBIOTICOS, "fa", Criticidade.ALTA, true),
                new Medicamento("MED-003", "Azitromicina 500mg", "Comprimido",
                        FamiliaTerapeutica.ANTIBIOTICOS, "cp", Criticidade.MEDIA, true),
                new Medicamento("MED-004", "Vancomicina 500mg", "Frasco-ampola",
                        FamiliaTerapeutica.ANTIBIOTICOS, "fa", Criticidade.ALTA, true),
                new Medicamento("MED-005", "Dipirona 500mg/mL", "Ampola 2mL",
                        FamiliaTerapeutica.ANALGESICOS, "amp", Criticidade.MEDIA, true),
                new Medicamento("MED-006", "Paracetamol 500mg", "Comprimido",
                        FamiliaTerapeutica.ANALGESICOS, "cp", Criticidade.BAIXA, true),
                new Medicamento("MED-007", "Morfina 10mg/mL", "Ampola 1mL",
                        FamiliaTerapeutica.ANALGESICOS, "amp", Criticidade.ALTA, true),
                new Medicamento("MED-008", "Tramadol 50mg/mL", "Ampola 2mL",
                        FamiliaTerapeutica.ANALGESICOS, "amp", Criticidade.MEDIA, true),
                new Medicamento("MED-009", "Oseltamivir 75mg", "Cápsula",
                        FamiliaTerapeutica.ANTIVIRAIS, "cap", Criticidade.ALTA, true),
                new Medicamento("MED-010", "Aciclovir 250mg", "Frasco-ampola",
                        FamiliaTerapeutica.ANTIVIRAIS, "fa", Criticidade.MEDIA, true),
                new Medicamento("MED-011", "Enalapril 10mg", "Comprimido",
                        FamiliaTerapeutica.CARDIOVASCULAR, "cp", Criticidade.MEDIA, true),
                new Medicamento("MED-012", "Losartana 50mg", "Comprimido",
                        FamiliaTerapeutica.CARDIOVASCULAR, "cp", Criticidade.MEDIA, true),
                new Medicamento("MED-013", "Furosemida 10mg/mL", "Ampola 2mL",
                        FamiliaTerapeutica.CARDIOVASCULAR, "amp", Criticidade.ALTA, true),
                new Medicamento("MED-014", "Noradrenalina 2mg/mL", "Ampola 4mL",
                        FamiliaTerapeutica.CARDIOVASCULAR, "amp", Criticidade.ALTA, true),
                new Medicamento("MED-015", "Heparina 5000UI/mL", "Frasco 5mL",
                        FamiliaTerapeutica.CARDIOVASCULAR, "fr", Criticidade.ALTA, true),
                new Medicamento("MED-016", "Soro Fisiológico 0,9% 500mL", "Bolsa",
                        FamiliaTerapeutica.SOROS_E_VACINAS, "bolsa", Criticidade.ALTA, true),
                new Medicamento("MED-017", "Ringer Lactato 500mL", "Bolsa",
                        FamiliaTerapeutica.SOROS_E_VACINAS, "bolsa", Criticidade.MEDIA, true),
                new Medicamento("MED-018", "Vacina Influenza", "Dose",
                        FamiliaTerapeutica.SOROS_E_VACINAS, "dose", Criticidade.MEDIA, true),
                new Medicamento("MED-019", "Soro Antiofídico Polivalente", "Ampola",
                        FamiliaTerapeutica.SOROS_E_VACINAS, "amp", Criticidade.ALTA, true),
                new Medicamento("MED-020", "Luva de Procedimento M", "Caixa 100un",
                        FamiliaTerapeutica.INSUMOS_MEDICOS, "cx", Criticidade.MEDIA, true),
                new Medicamento("MED-021", "Seringa 10mL", "Unidade",
                        FamiliaTerapeutica.INSUMOS_MEDICOS, "un", Criticidade.BAIXA, true),
                new Medicamento("MED-022", "Cateter Venoso 20G", "Unidade",
                        FamiliaTerapeutica.INSUMOS_MEDICOS, "un", Criticidade.MEDIA, true),
                new Medicamento("MED-023", "Máscara Cirúrgica", "Caixa 50un",
                        FamiliaTerapeutica.INSUMOS_MEDICOS, "cx", Criticidade.BAIXA, true),
                new Medicamento("MED-024", "Haloperidol 5mg/mL", "Ampola 1mL",
                        FamiliaTerapeutica.SAUDE_MENTAL, "amp", Criticidade.MEDIA, true),
                new Medicamento("MED-025", "Diazepam 5mg/mL", "Ampola 2mL",
                        FamiliaTerapeutica.SAUDE_MENTAL, "amp", Criticidade.MEDIA, true),
                new Medicamento("MED-026", "Clorpromazina 25mg", "Comprimido",
                        FamiliaTerapeutica.SAUDE_MENTAL, "cp", Criticidade.BAIXA, false),
                new Medicamento("MED-027", "Artesunato + Mefloquina", "Comprimido",
                        FamiliaTerapeutica.ANTIPARASITARIOS, "cp", Criticidade.ALTA, true),
                new Medicamento("MED-028", "Albendazol 400mg", "Comprimido",
                        FamiliaTerapeutica.ANTIPARASITARIOS, "cp", Criticidade.BAIXA, false),
                new Medicamento("MED-029", "Ivermectina 6mg", "Comprimido",
                        FamiliaTerapeutica.ANTIPARASITARIOS, "cp", Criticidade.BAIXA, false),
                new Medicamento("MED-030", "Doxiciclina 100mg", "Comprimido",
                        FamiliaTerapeutica.ANTIBIOTICOS, "cp", Criticidade.MEDIA, true)
        );

        int inseridos = 0;
        for (Medicamento m : catalogo) {
            if (!medicamentoRepository.existsByCodigoIgnoreCase(m.getCodigo())) {
                medicamentoRepository.save(m);
                inseridos++;
            }
        }
        if (inseridos > 0) {
            log.info("Catalogo de medicamentos semeado: {} novos inseridos (total: {}).",
                    inseridos, medicamentoRepository.count());
        } else {
            log.info("Catalogo de medicamentos ja semeado (total: {}). Nada a fazer.",
                    medicamentoRepository.count());
        }
    }
}
