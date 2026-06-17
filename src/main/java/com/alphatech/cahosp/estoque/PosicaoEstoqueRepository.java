package com.alphatech.cahosp.estoque;

import com.alphatech.cahosp.estoque.dominio.PosicaoEstoque;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    /**
     * Todas as posicoes com medicamento e unidade ja carregados (fetch join) — para os motores de
     * geracao (alerta/recomendacao) varrerem o estoque sem disparar N+1 nas relacoes lazy.
     */
    @Query("SELECT p FROM PosicaoEstoque p JOIN FETCH p.medicamento JOIN FETCH p.unidade")
    List<PosicaoEstoque> findAllComRelacionamentos();

    /** Total de posicoes em nivel critico (saldo abaixo do nivel critico). RF-DASH/RF-EST-04. */
    @Query("SELECT COUNT(p) FROM PosicaoEstoque p WHERE p.quantidade < p.nivelCritico")
    long contarCriticos();

    /** Variante de {@link #contarCriticos} com filtro opcional de unidade/medicamento (painel). */
    @Query("""
            SELECT COUNT(p) FROM PosicaoEstoque p
            WHERE p.quantidade < p.nivelCritico
              AND (:unidadeId IS NULL OR p.unidade.id = :unidadeId)
              AND (:medicamentoId IS NULL OR p.medicamento.id = :medicamentoId)
            """)
    long contarCriticosFiltrado(@Param("unidadeId") UUID unidadeId,
                                @Param("medicamentoId") UUID medicamentoId);

    /** Medicamentos distintos com posicao no escopo (filtros opcionais de unidade/medicamento). */
    @Query("""
            SELECT COUNT(DISTINCT p.medicamento.id) FROM PosicaoEstoque p
            WHERE (:unidadeId IS NULL OR p.unidade.id = :unidadeId)
              AND (:medicamentoId IS NULL OR p.medicamento.id = :medicamentoId)
            """)
    long contarMedicamentosDistintos(@Param("unidadeId") UUID unidadeId,
                                     @Param("medicamentoId") UUID medicamentoId);

    /** Posicoes de uma unidade (resumo operacional do painel). RF-DASH-02. */
    List<PosicaoEstoque> findByUnidadeId(UUID unidadeId);

    /** Posicoes com filtros opcionais de unidade/medicamento (KPIs do resumo de estoque filtrado). */
    @Query("""
            SELECT p FROM PosicaoEstoque p
            WHERE (:unidadeId IS NULL OR p.unidade.id = :unidadeId)
              AND (:medicamentoId IS NULL OR p.medicamento.id = :medicamentoId)
            """)
    List<PosicaoEstoque> buscarFiltrado(@Param("unidadeId") UUID unidadeId,
                                        @Param("medicamentoId") UUID medicamentoId);

    /**
     * Medicamentos com mais posicoes criticas — base para escolher o item do grafico agregado do
     * dashboard (RF-DASH/RF-PRV-02). Retorna pares [medicamentoId, totalCriticos]. O filtro
     * opcional de unidade restringe o ranking ao escopo do dashboard filtrado.
     */
    @Query("""
            SELECT p.medicamento.id, COUNT(p) FROM PosicaoEstoque p
            WHERE p.quantidade < p.nivelCritico
              AND (:unidadeId IS NULL OR p.unidade.id = :unidadeId)
            GROUP BY p.medicamento.id
            ORDER BY COUNT(p) DESC
            """)
    List<Object[]> contarCriticosPorMedicamento(@Param("unidadeId") UUID unidadeId, Pageable pageable);
}
