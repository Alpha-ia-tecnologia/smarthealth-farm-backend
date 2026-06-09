package com.alphatech.cahosp.estoque;

import com.alphatech.cahosp.estoque.dominio.PosicaoEstoque;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PosicaoEstoqueRepository extends JpaRepository<PosicaoEstoque, UUID> {

    Optional<PosicaoEstoque> findByMedicamentoIdAndUnidadeId(UUID medicamentoId, UUID unidadeId);

    /**
     * Posicoes com filtros opcionais (unidade, medicamento e busca por nome do medicamento,
     * nome ou sigla da unidade). O filtro de status (ok/atencao/critico) e aplicado no servico,
     * pois e derivado. RF-EST-01.
     */
    @Query("""
            SELECT p FROM PosicaoEstoque p
            WHERE (:unidadeId IS NULL OR p.unidade.id = :unidadeId)
              AND (:medicamentoId IS NULL OR p.medicamento.id = :medicamentoId)
              AND (:busca IS NULL
                   OR LOWER(p.medicamento.nome) LIKE LOWER(CONCAT('%', CAST(:busca AS string), '%'))
                   OR LOWER(p.unidade.nome) LIKE LOWER(CONCAT('%', CAST(:busca AS string), '%'))
                   OR LOWER(p.unidade.sigla) LIKE LOWER(CONCAT('%', CAST(:busca AS string), '%')))
            """)
    List<PosicaoEstoque> buscarComFiltros(@Param("unidadeId") UUID unidadeId,
                                          @Param("medicamentoId") UUID medicamentoId,
                                          @Param("busca") String busca,
                                          Sort sort);
}
