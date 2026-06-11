package com.alphatech.cahosp.estoque;

import com.alphatech.cahosp.estoque.dominio.PosicaoEstoque;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Persistencia de posicoes de estoque. A listagem paginada/filtrada usa
 * {@link JpaSpecificationExecutor} (ver {@link EspecificacoesPosicao}), inclusive o filtro de
 * status derivado, para paginar corretamente no banco.
 */
public interface PosicaoEstoqueRepository
        extends JpaRepository<PosicaoEstoque, UUID>, JpaSpecificationExecutor<PosicaoEstoque> {

    Optional<PosicaoEstoque> findByMedicamentoIdAndUnidadeId(UUID medicamentoId, UUID unidadeId);

    /** Total de posicoes em nivel critico (saldo abaixo do nivel critico). RF-DASH/RF-EST-04. */
    @Query("SELECT COUNT(p) FROM PosicaoEstoque p WHERE p.quantidade < p.nivelCritico")
    long contarCriticos();

    /** Posicoes de uma unidade (resumo operacional do painel). RF-DASH-02. */
    List<PosicaoEstoque> findByUnidadeId(UUID unidadeId);

    /**
     * Medicamentos com mais posicoes criticas — base para escolher o item do grafico agregado do
     * dashboard (RF-DASH/RF-PRV-02). Retorna pares [medicamentoId, totalCriticos].
     */
    @Query("""
            SELECT p.medicamento.id, COUNT(p) FROM PosicaoEstoque p
            WHERE p.quantidade < p.nivelCritico
            GROUP BY p.medicamento.id
            ORDER BY COUNT(p) DESC
            """)
    List<Object[]> contarCriticosPorMedicamento(Pageable pageable);
}
