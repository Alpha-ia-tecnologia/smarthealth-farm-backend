package com.alphatech.cahosp.alerta;

import com.alphatech.cahosp.alerta.dominio.Alerta;
import com.alphatech.cahosp.alerta.dominio.Severidade;
import com.alphatech.cahosp.alerta.dominio.StatusAlerta;
import com.alphatech.cahosp.alerta.dominio.TipoAlerta;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AlertaRepository extends JpaRepository<Alerta, UUID> {

    /**
     * Lista alertas, paginada, com filtros opcionais (tipo, severidade, status, unidade,
     * insumo, busca). Insumo, unidade e lote vem em fetch join (to-one, compativel com
     * paginacao no banco); os destinatarios (colecao EAGER) carregam por linha — volume pequeno
     * por pagina. RF-ALE-01/02.
     */
    @Query(value = """
            SELECT a FROM Alerta a
              JOIN FETCH a.insumo m
              JOIN FETCH a.unidade u
              LEFT JOIN FETCH a.lote l
            WHERE (:tipo IS NULL OR a.tipo = :tipo)
              AND (:severidade IS NULL OR a.severidade = :severidade)
              AND (:status IS NULL OR a.status = :status)
              AND (:unidadeId IS NULL OR u.id = :unidadeId)
              AND (:insumoId IS NULL OR m.id = :insumoId)
              AND (:busca IS NULL
                   OR LOWER(m.nome) LIKE LOWER(CONCAT('%', CAST(:busca AS string), '%'))
                   OR LOWER(u.nome) LIKE LOWER(CONCAT('%', CAST(:busca AS string), '%'))
                   OR LOWER(u.sigla) LIKE LOWER(CONCAT('%', CAST(:busca AS string), '%'))
                   OR LOWER(a.mensagem) LIKE LOWER(CONCAT('%', CAST(:busca AS string), '%')))
            """,
            countQuery = """
            SELECT COUNT(a) FROM Alerta a
              JOIN a.insumo m
              JOIN a.unidade u
            WHERE (:tipo IS NULL OR a.tipo = :tipo)
              AND (:severidade IS NULL OR a.severidade = :severidade)
              AND (:status IS NULL OR a.status = :status)
              AND (:unidadeId IS NULL OR u.id = :unidadeId)
              AND (:insumoId IS NULL OR m.id = :insumoId)
              AND (:busca IS NULL
                   OR LOWER(m.nome) LIKE LOWER(CONCAT('%', CAST(:busca AS string), '%'))
                   OR LOWER(u.nome) LIKE LOWER(CONCAT('%', CAST(:busca AS string), '%'))
                   OR LOWER(u.sigla) LIKE LOWER(CONCAT('%', CAST(:busca AS string), '%'))
                   OR LOWER(a.mensagem) LIKE LOWER(CONCAT('%', CAST(:busca AS string), '%')))
            """)
    Page<Alerta> buscarComFiltros(@Param("tipo") TipoAlerta tipo,
                                  @Param("severidade") Severidade severidade,
                                  @Param("status") StatusAlerta status,
                                  @Param("unidadeId") UUID unidadeId,
                                  @Param("insumoId") UUID insumoId,
                                  @Param("busca") String busca,
                                  Pageable pageable);

    /** Alerta com os relacionamentos carregados (drill-down / resposta apos mudanca de status). */
    @Query("""
            SELECT DISTINCT a FROM Alerta a
              JOIN FETCH a.insumo
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
              JOIN FETCH a.insumo
              JOIN FETCH a.unidade
              LEFT JOIN FETCH a.lote
              LEFT JOIN FETCH a.destinatarios
            WHERE a.status <> :status
            ORDER BY a.diasParaEvento ASC
            """)
    List<Alerta> findUrgentesNaoResolvidos(@Param("status") StatusAlerta status, Pageable pageable);

    // ----- Painel filtrado por unidade/insumo (RF-DASH-01/02) -----

    /**
     * Conta alertas para o painel aplicando filtros opcionais. Um unico metodo cobre os varios
     * KPIs: {@code status} casa por igualdade (ex.: ABERTO) e {@code statusExcluido} por diferenca
     * (ex.: <> RESOLVIDO = "ativos"); {@code tipo}, {@code unidadeId} e {@code insumoId} sao
     * opcionais. Com todos nulos, equivale a contagem da rede inteira.
     */
    @Query("""
            SELECT COUNT(a) FROM Alerta a
            WHERE (:tipo IS NULL OR a.tipo = :tipo)
              AND (:severidade IS NULL OR a.severidade = :severidade)
              AND (:status IS NULL OR a.status = :status)
              AND (:statusExcluido IS NULL OR a.status <> :statusExcluido)
              AND (:unidadeId IS NULL OR a.unidade.id = :unidadeId)
              AND (:insumoId IS NULL OR a.insumo.id = :insumoId)
            """)
    long contarPainel(@Param("tipo") TipoAlerta tipo,
                      @Param("severidade") Severidade severidade,
                      @Param("status") StatusAlerta status,
                      @Param("statusExcluido") StatusAlerta statusExcluido,
                      @Param("unidadeId") UUID unidadeId,
                      @Param("insumoId") UUID insumoId);

    /** Variante de {@link #findUrgentesNaoResolvidos} com filtro opcional de unidade/insumo. */
    @Query("""
            SELECT DISTINCT a FROM Alerta a
              JOIN FETCH a.insumo
              JOIN FETCH a.unidade
              LEFT JOIN FETCH a.lote
              LEFT JOIN FETCH a.destinatarios
            WHERE a.status <> :status
              AND (:unidadeId IS NULL OR a.unidade.id = :unidadeId)
              AND (:insumoId IS NULL OR a.insumo.id = :insumoId)
            ORDER BY a.diasParaEvento ASC
            """)
    List<Alerta> findUrgentesNaoResolvidosFiltrado(@Param("status") StatusAlerta status,
                                                   @Param("unidadeId") UUID unidadeId,
                                                   @Param("insumoId") UUID insumoId,
                                                   Pageable pageable);

    // ----- Suporte ao motor de geracao (idempotencia por chave natural) -----

    /**
     * Chaves [insumoId, unidadeId] dos alertas existentes de um tipo sem lote (desabastecimento).
     * Carregadas de uma vez para o motor deduplicar em memoria — evita um {@code exists} por posicao
     * (N+1). RF-ALE-01.
     */
    @Query("SELECT a.insumo.id, a.unidade.id FROM Alerta a WHERE a.tipo = :tipo AND a.lote IS NULL")
    List<Object[]> chavesPorInsumoEUnidade(@Param("tipo") TipoAlerta tipo);

    /** Ids dos lotes que ja possuem alerta de um tipo (vencimento) — dedup em memoria, sem N+1. */
    @Query("SELECT a.lote.id FROM Alerta a WHERE a.tipo = :tipo AND a.lote IS NOT NULL")
    List<UUID> lotesComAlerta(@Param("tipo") TipoAlerta tipo);

    /**
     * Remove os alertas em determinado status (usado para renovar os abertos na regeneracao).
     * Delete derivado (carrega e remove as entidades) — cascateia para os destinatarios
     * ({@code alerta_destinatario}), ao contrario de um bulk delete JPQL.
     */
    long deleteByStatus(StatusAlerta status);
}
