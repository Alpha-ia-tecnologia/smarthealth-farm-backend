package com.alphatech.cahosp.estoque;

import com.alphatech.cahosp.estoque.dominio.Movimentacao;
import com.alphatech.cahosp.estoque.dominio.TipoMovimentacao;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface MovimentacaoRepository extends JpaRepository<Movimentacao, UUID> {

    /**
     * Movimentacoes recentes de uma posicao (drill-down), mais novas primeiro. O {@code Pageable}
     * limita o volume — o livro-razao cresce sem limite. RF-EST-06.
     */
    List<Movimentacao> findByInsumoIdAndUnidadeIdOrderByDataHoraDesc(
            UUID insumoId, UUID unidadeId, Pageable pageable);

    /**
     * Movimentacoes com filtros opcionais (insumo, unidade, lote, tipo), paginadas. RF-EST-06.
     */
    @Query("""
            SELECT m FROM Movimentacao m
            WHERE (:insumoId IS NULL OR m.insumo.id = :insumoId)
              AND (:unidadeId IS NULL OR m.unidade.id = :unidadeId)
              AND (:loteId IS NULL OR m.lote.id = :loteId)
              AND (:tipo IS NULL OR m.tipo = :tipo)
            """)
    Page<Movimentacao> buscarComFiltros(@Param("insumoId") UUID insumoId,
                                        @Param("unidadeId") UUID unidadeId,
                                        @Param("loteId") UUID loteId,
                                        @Param("tipo") TipoMovimentacao tipo,
                                        Pageable pageable);
}
