package com.alphatech.cahosp.estoque;

import com.alphatech.cahosp.estoque.dominio.Lote;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface LoteRepository extends JpaRepository<Lote, UUID> {

    List<Lote> findByMedicamentoIdAndUnidadeIdOrderByValidadeAsc(UUID medicamentoId, UUID unidadeId);

    boolean existsByNumeroLoteIgnoreCase(String numeroLote);

    /**
     * Lotes com filtros opcionais: unidade, medicamento, apenas com saldo, e validade ate uma
     * data (controle de vencimento — RF-EST-02). RF-EST-01.
     */
    @Query("""
            SELECT l FROM Lote l
            WHERE (:unidadeId IS NULL OR l.unidade.id = :unidadeId)
              AND (:medicamentoId IS NULL OR l.medicamento.id = :medicamentoId)
              AND (:apenasComSaldo = false OR l.quantidade > 0)
              AND (:validadeAte IS NULL OR l.validade <= :validadeAte)
            """)
    List<Lote> buscarComFiltros(@Param("unidadeId") UUID unidadeId,
                                @Param("medicamentoId") UUID medicamentoId,
                                @Param("apenasComSaldo") boolean apenasComSaldo,
                                @Param("validadeAte") LocalDate validadeAte,
                                Sort sort);

    long countByQuantidadeGreaterThanAndValidadeLessThanEqual(int quantidade, LocalDate validadeAte);
}
