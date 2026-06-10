package com.alphatech.cahosp.estoque;

import com.alphatech.cahosp.estoque.dominio.Lote;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Filtro dinamico de lotes (unidade, medicamento, apenas com saldo, validade ate uma data).
 *
 * <p>Usa a Criteria API (Specification) em vez de JPQL com {@code :param IS NULL}: o filtro de
 * <strong>data</strong> nullable ({@code validadeAte}) quebrava no PostgreSQL com
 * <em>"could not determine data type of parameter"</em> quando o parametro vinha nulo. Aqui cada
 * predicado ausente e simplesmente omitido — sem parametro de tipo indeterminado. RF-EST-01/02.
 */
final class EspecificacoesLote {

    private EspecificacoesLote() {
    }

    static Specification<Lote> comFiltros(UUID unidadeId, UUID medicamentoId,
                                          boolean apenasComSaldo, LocalDate validadeAte) {
        return (root, query, cb) -> {
            List<Predicate> predicados = new ArrayList<>();
            if (unidadeId != null) {
                predicados.add(cb.equal(root.get("unidade").get("id"), unidadeId));
            }
            if (medicamentoId != null) {
                predicados.add(cb.equal(root.get("medicamento").get("id"), medicamentoId));
            }
            if (apenasComSaldo) {
                predicados.add(cb.greaterThan(root.get("quantidade"), 0));
            }
            if (validadeAte != null) {
                predicados.add(cb.lessThanOrEqualTo(root.get("validade"), validadeAte));
            }
            return cb.and(predicados.toArray(Predicate[]::new));
        };
    }
}
