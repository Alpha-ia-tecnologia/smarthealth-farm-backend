package com.alphatech.cahosp.alerta;

import com.alphatech.cahosp.alerta.dominio.Alerta;
import com.alphatech.cahosp.alerta.dominio.Severidade;
import com.alphatech.cahosp.alerta.dominio.StatusAlerta;
import com.alphatech.cahosp.alerta.dominio.TipoAlerta;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AlertaRepository extends JpaRepository<Alerta, UUID> {

    /**
     * Lista alertas com filtros opcionais (tipo, severidade, status, unidade, medicamento, busca).
     * Medicamento, unidade e lote vem em fetch join para evitar N+1 ao montar a resposta
     * (nomes, sigla, numero do lote). RF-ALE-01/02.
     */
    @Query("""
            SELECT DISTINCT a FROM Alerta a
              JOIN FETCH a.medicamento m
              JOIN FETCH a.unidade u
              LEFT JOIN FETCH a.lote l
              LEFT JOIN FETCH a.destinatarios
            WHERE (:tipo IS NULL OR a.tipo = :tipo)
              AND (:severidade IS NULL OR a.severidade = :severidade)
              AND (:status IS NULL OR a.status = :status)
              AND (:unidadeId IS NULL OR u.id = :unidadeId)
              AND (:medicamentoId IS NULL OR m.id = :medicamentoId)
              AND (:busca IS NULL
                   OR LOWER(m.nome) LIKE LOWER(CONCAT('%', CAST(:busca AS string), '%'))
                   OR LOWER(u.nome) LIKE LOWER(CONCAT('%', CAST(:busca AS string), '%'))
                   OR LOWER(u.sigla) LIKE LOWER(CONCAT('%', CAST(:busca AS string), '%'))
                   OR LOWER(a.mensagem) LIKE LOWER(CONCAT('%', CAST(:busca AS string), '%')))
            """)
    List<Alerta> buscarComFiltros(@Param("tipo") TipoAlerta tipo,
                                  @Param("severidade") Severidade severidade,
                                  @Param("status") StatusAlerta status,
                                  @Param("unidadeId") UUID unidadeId,
                                  @Param("medicamentoId") UUID medicamentoId,
                                  @Param("busca") String busca,
                                  Sort sort);

    /** Alerta com os relacionamentos carregados (drill-down / resposta apos mudanca de status). */
    @Query("""
            SELECT DISTINCT a FROM Alerta a
              JOIN FETCH a.medicamento
              JOIN FETCH a.unidade
              LEFT JOIN FETCH a.lote
              LEFT JOIN FETCH a.destinatarios
            WHERE a.id = :id
            """)
    Optional<Alerta> findComRelacionamentos(@Param("id") UUID id);

    // ----- KPIs do painel (RF-ALE-04/05) -----

    long countByStatus(StatusAlerta status);

    long countByTipoAndStatusNot(TipoAlerta tipo, StatusAlerta status);

    long countBySeveridadeAndStatusNot(Severidade severidade, StatusAlerta status);

    /** Alertas ativos (nao resolvidos) de uma unidade — usado no resumo por unidade do painel. */
    long countByUnidadeIdAndStatusNot(UUID unidadeId, StatusAlerta status);

    /**
     * Alertas mais urgentes ainda nao resolvidos (fila do painel), com relacionamentos
     * carregados para montar a resposta sem N+1. RF-DASH-01/02.
     */
    @Query("""
            SELECT DISTINCT a FROM Alerta a
              JOIN FETCH a.medicamento
              JOIN FETCH a.unidade
              LEFT JOIN FETCH a.lote
              LEFT JOIN FETCH a.destinatarios
            WHERE a.status <> :status
            ORDER BY a.diasParaEvento ASC
            """)
    List<Alerta> findUrgentesNaoResolvidos(@Param("status") StatusAlerta status, Pageable pageable);

    // ----- Suporte ao motor de geracao (idempotencia por chave natural) -----

    /** Ja existe um alerta de desabastecimento (qualquer status) para este medicamento/unidade? */
    boolean existsByTipoAndMedicamentoIdAndUnidadeIdAndLoteIsNull(
            TipoAlerta tipo, UUID medicamentoId, UUID unidadeId);

    /** Ja existe um alerta de vencimento (qualquer status) para este lote? */
    boolean existsByTipoAndLoteId(TipoAlerta tipo, UUID loteId);

    /**
     * Remove os alertas em determinado status (usado para renovar os abertos na regeneracao).
     * Delete derivado (carrega e remove as entidades) — cascateia para os destinatarios
     * ({@code alerta_destinatario}), ao contrario de um bulk delete JPQL.
     */
    long deleteByStatus(StatusAlerta status);
}
