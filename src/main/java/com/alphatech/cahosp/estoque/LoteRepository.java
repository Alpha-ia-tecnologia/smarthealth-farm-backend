package com.alphatech.cahosp.estoque;

import com.alphatech.cahosp.estoque.dominio.Lote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Persistencia de lotes. O filtro dinamico da listagem usa {@link JpaSpecificationExecutor}
 * (ver {@link EspecificacoesLote}) — evita o {@code :param IS NULL} de tipo indeterminado no
 * PostgreSQL para o filtro de data nullable {@code validadeAte}.
 */
public interface LoteRepository extends JpaRepository<Lote, UUID>, JpaSpecificationExecutor<Lote> {

    List<Lote> findByInsumoIdAndUnidadeIdOrderByValidadeAsc(UUID insumoId, UUID unidadeId);

    boolean existsByNumeroLoteIgnoreCase(String numeroLote);

    long countByQuantidadeGreaterThanAndValidadeLessThanEqual(int quantidade, LocalDate validadeAte);

    /**
     * Conta lotes com saldo proximos do vencimento (ate {@code validadeAte}), com filtro opcional
     * de unidade/insumo — KPI do painel filtrado. RF-DASH-01/02.
     */
    @Query("""
            SELECT COUNT(l) FROM Lote l
            WHERE l.quantidade > 0 AND l.validade <= :validadeAte
              AND (:unidadeId IS NULL OR l.unidade.id = :unidadeId)
              AND (:insumoId IS NULL OR l.insumo.id = :insumoId)
            """)
    long contarProximosVencimento(@Param("validadeAte") LocalDate validadeAte,
                                  @Param("unidadeId") UUID unidadeId,
                                  @Param("insumoId") UUID insumoId);

    /**
     * Total de lotes com saldo no escopo (filtros opcionais de unidade/insumo) — denominador das
     * Perdas por vencimento (RF-IND). RF-EST/RF-IND.
     */
    @Query("""
            SELECT COUNT(l) FROM Lote l
            WHERE l.quantidade > 0
              AND (:unidadeId IS NULL OR l.unidade.id = :unidadeId)
              AND (:insumoId IS NULL OR l.insumo.id = :insumoId)
            """)
    long contarComSaldo(@Param("unidadeId") UUID unidadeId,
                        @Param("insumoId") UUID insumoId);

    /**
     * Lotes com saldo <strong>ja vencidos</strong> (validade anterior a {@code referencia}) no
     * escopo — numerador (aproximacao de perda) das Perdas por vencimento (RF-IND). Estoque vencido
     * ainda em maos = desperdicio.
     */
    @Query("""
            SELECT COUNT(l) FROM Lote l
            WHERE l.quantidade > 0 AND l.validade < :referencia
              AND (:unidadeId IS NULL OR l.unidade.id = :unidadeId)
              AND (:insumoId IS NULL OR l.insumo.id = :insumoId)
            """)
    long contarVencidosComSaldo(@Param("referencia") LocalDate referencia,
                                @Param("unidadeId") UUID unidadeId,
                                @Param("insumoId") UUID insumoId);

    /**
     * Lotes com saldo acima de {@code quantidade} e validade ate a data, ordenados pela validade,
     * com insumo e unidade ja carregados (fetch join). Usado pelo motor de alertas de
     * vencimento (RF-ALE-02) — evita N+1 nas relacoes lazy ao varrer os lotes.
     */
    @Query("""
            SELECT l FROM Lote l
              JOIN FETCH l.insumo
              JOIN FETCH l.unidade
            WHERE l.quantidade > :quantidade AND l.validade <= :validadeAte
            ORDER BY l.validade ASC
            """)
    List<Lote> findVencendoComRelacionamentos(@Param("quantidade") int quantidade,
                                              @Param("validadeAte") LocalDate validadeAte);
}
