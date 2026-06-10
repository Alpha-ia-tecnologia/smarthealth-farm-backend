package com.alphatech.cahosp.estoque;

import com.alphatech.cahosp.estoque.dominio.Lote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Persistencia de lotes. O filtro dinamico da listagem usa {@link JpaSpecificationExecutor}
 * (ver {@link EspecificacoesLote}) — evita o {@code :param IS NULL} de tipo indeterminado no
 * PostgreSQL para o filtro de data nullable {@code validadeAte}.
 */
public interface LoteRepository extends JpaRepository<Lote, UUID>, JpaSpecificationExecutor<Lote> {

    List<Lote> findByMedicamentoIdAndUnidadeIdOrderByValidadeAsc(UUID medicamentoId, UUID unidadeId);

    boolean existsByNumeroLoteIgnoreCase(String numeroLote);

    long countByQuantidadeGreaterThanAndValidadeLessThanEqual(int quantidade, LocalDate validadeAte);

    /**
     * Lotes com saldo acima de {@code quantidade} e validade ate a data, ordenados pela validade.
     * Usado pelo motor de alertas de vencimento (RF-ALE-02).
     */
    List<Lote> findByQuantidadeGreaterThanAndValidadeLessThanEqualOrderByValidadeAsc(
            int quantidade, LocalDate validadeAte);
}
