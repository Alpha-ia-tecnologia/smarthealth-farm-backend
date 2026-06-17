package com.alphatech.cahosp.estoque;

import com.alphatech.cahosp.estoque.dominio.PosicaoEstoque;
import com.alphatech.cahosp.estoque.dominio.StatusEstoque;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Filtro dinamico de posicoes de estoque (unidade, insumo, busca e <strong>status</strong>).
 *
 * <p>O {@code status} (ok/atencao/critico) e <em>derivado</em> de {@code quantidade} vs.
 * {@code nivelCritico} (mesma regra de {@link CalculadoraEstoque}). Empurrar essa regra para a
 * query (Criteria API) e o que permite <strong>paginar no banco</strong> com o filtro de status
 * correto — se filtrasse o status em memoria apos a pagina, a contagem de paginas sairia errada.
 * RF-EST-01/04.
 */
final class EspecificacoesPosicao {

    private EspecificacoesPosicao() {
    }

    static Specification<PosicaoEstoque> comFiltros(UUID unidadeId, UUID insumoId,
                                                    StatusEstoque status, String busca) {
        return (root, query, cb) -> {
            List<Predicate> predicados = new ArrayList<>();

            if (unidadeId != null) {
                predicados.add(cb.equal(root.get("unidade").get("id"), unidadeId));
            }
            if (insumoId != null) {
                predicados.add(cb.equal(root.get("insumo").get("id"), insumoId));
            }
            if (busca != null && !busca.isBlank()) {
                String termo = "%" + busca.trim().toLowerCase() + "%";
                predicados.add(cb.or(
                        cb.like(cb.lower(root.get("insumo").get("nome")), termo),
                        cb.like(cb.lower(root.get("unidade").get("nome")), termo),
                        cb.like(cb.lower(root.get("unidade").get("sigla")), termo)));
            }
            if (status != null) {
                Expression<Integer> quantidade = root.get("quantidade");
                Expression<Integer> nivelCritico = root.get("nivelCritico");
                // Limiar de "atencao": nivelCritico * 1.25 (mesmo fator do front/CalculadoraEstoque).
                Expression<Double> limiarAtencao =
                        cb.prod(nivelCritico.as(Double.class), CalculadoraEstoque.FATOR_ATENCAO);
                Expression<Double> quantidadeD = quantidade.as(Double.class);

                predicados.add(switch (status) {
                    case CRITICO -> cb.lessThan(quantidade, nivelCritico);
                    case ATENCAO -> cb.and(
                            cb.greaterThanOrEqualTo(quantidade, nivelCritico),
                            cb.lessThan(quantidadeD, limiarAtencao));
                    case OK -> cb.greaterThanOrEqualTo(quantidadeD, limiarAtencao);
                });
            }

            return cb.and(predicados.toArray(Predicate[]::new));
        };
    }
}
