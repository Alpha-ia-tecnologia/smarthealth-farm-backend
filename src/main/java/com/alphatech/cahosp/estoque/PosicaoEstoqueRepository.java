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

    Optional<PosicaoEstoque> findByInsumoIdAndUnidadeId(UUID insumoId, UUID unidadeId);

    /**
     * Todas as posicoes com insumo e unidade ja carregados (fetch join) — para os motores de
     * geracao (alerta/recomendacao) varrerem o estoque sem disparar N+1 nas relacoes lazy.
     */
    @Query("SELECT p FROM PosicaoEstoque p JOIN FETCH p.insumo JOIN FETCH p.unidade")
    List<PosicaoEstoque> findAllComRelacionamentos();

    /** Total de posicoes em nivel critico (saldo abaixo do nivel critico). RF-DASH/RF-EST-04. */
    @Query("SELECT COUNT(p) FROM PosicaoEstoque p WHERE p.quantidade < p.nivelCritico")
    long contarCriticos();

    /** Variante de {@link #contarCriticos} com filtro opcional de unidade/insumo (painel). */
    @Query("""
            SELECT COUNT(p) FROM PosicaoEstoque p
            WHERE p.quantidade < p.nivelCritico
              AND (:unidadeId IS NULL OR p.unidade.id = :unidadeId)
              AND (:insumoId IS NULL OR p.insumo.id = :insumoId)
            """)
    long contarCriticosFiltrado(@Param("unidadeId") UUID unidadeId,
                                @Param("insumoId") UUID insumoId);

    /** Insumos distintos com posicao no escopo (filtros opcionais de unidade/insumo). */
    @Query("""
            SELECT COUNT(DISTINCT p.insumo.id) FROM PosicaoEstoque p
            WHERE (:unidadeId IS NULL OR p.unidade.id = :unidadeId)
              AND (:insumoId IS NULL OR p.insumo.id = :insumoId)
            """)
    long contarInsumosDistintos(@Param("unidadeId") UUID unidadeId,
                                     @Param("insumoId") UUID insumoId);

    /** Posicoes de uma unidade (resumo operacional do painel). RF-DASH-02. */
    List<PosicaoEstoque> findByUnidadeId(UUID unidadeId);

    /**
     * Consumo medio diario total por insumo (somado sobre as unidades da rede) — base da Curva ABC
     * (RF-EST). Retorna pares [insumoId, somaConsumo].
     */
    @Query("""
            SELECT p.insumo.id, SUM(p.consumoMedioDiario) FROM PosicaoEstoque p
            GROUP BY p.insumo.id
            """)
    List<Object[]> somarConsumoPorInsumo();

    /** Posicoes com filtros opcionais de unidade/insumo (KPIs do resumo de estoque filtrado). */
    @Query("""
            SELECT p FROM PosicaoEstoque p
            WHERE (:unidadeId IS NULL OR p.unidade.id = :unidadeId)
              AND (:insumoId IS NULL OR p.insumo.id = :insumoId)
            """)
    List<PosicaoEstoque> buscarFiltrado(@Param("unidadeId") UUID unidadeId,
                                        @Param("insumoId") UUID insumoId);

    /**
     * Insumos com mais posicoes criticas — base para escolher o item do grafico agregado do
     * dashboard (RF-DASH/RF-PRV-02). Retorna pares [insumoId, totalCriticos]. O filtro
     * opcional de unidade restringe o ranking ao escopo do dashboard filtrado.
     */
    @Query("""
            SELECT p.insumo.id, COUNT(p) FROM PosicaoEstoque p
            WHERE p.quantidade < p.nivelCritico
              AND (:unidadeId IS NULL OR p.unidade.id = :unidadeId)
            GROUP BY p.insumo.id
            ORDER BY COUNT(p) DESC
            """)
    List<Object[]> contarCriticosPorInsumo(@Param("unidadeId") UUID unidadeId, Pageable pageable);
}
