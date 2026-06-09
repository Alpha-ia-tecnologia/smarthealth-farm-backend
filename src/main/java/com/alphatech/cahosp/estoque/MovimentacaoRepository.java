package com.alphatech.cahosp.estoque;

import com.alphatech.cahosp.estoque.dominio.Movimentacao;
import com.alphatech.cahosp.estoque.dominio.TipoMovimentacao;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface MovimentacaoRepository extends JpaRepository<Movimentacao, UUID> {

    List<Movimentacao> findByMedicamentoIdAndUnidadeIdOrderByDataHoraDesc(UUID medicamentoId, UUID unidadeId);

    /**
     * Movimentacoes com filtros opcionais (medicamento, unidade, lote, tipo). RF-EST-06.
     */
    @Query("""
            SELECT m FROM Movimentacao m
            WHERE (:medicamentoId IS NULL OR m.medicamento.id = :medicamentoId)
              AND (:unidadeId IS NULL OR m.unidade.id = :unidadeId)
              AND (:loteId IS NULL OR m.lote.id = :loteId)
              AND (:tipo IS NULL OR m.tipo = :tipo)
            """)
    List<Movimentacao> buscarComFiltros(@Param("medicamentoId") UUID medicamentoId,
                                        @Param("unidadeId") UUID unidadeId,
                                        @Param("loteId") UUID loteId,
                                        @Param("tipo") TipoMovimentacao tipo,
                                        Sort sort);
}
