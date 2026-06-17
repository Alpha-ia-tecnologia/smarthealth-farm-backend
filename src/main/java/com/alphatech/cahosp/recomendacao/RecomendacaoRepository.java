package com.alphatech.cahosp.recomendacao;

import com.alphatech.cahosp.recomendacao.dominio.OrigemMotor;
import com.alphatech.cahosp.recomendacao.dominio.Prioridade;
import com.alphatech.cahosp.recomendacao.dominio.Recomendacao;
import com.alphatech.cahosp.recomendacao.dominio.StatusRecomendacao;
import com.alphatech.cahosp.recomendacao.dominio.TipoRecomendacao;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RecomendacaoRepository extends JpaRepository<Recomendacao, UUID> {

    /**
     * Lista recomendacoes, paginada, com filtros opcionais (tipo, status, origem do motor,
     * prioridade, unidade — destino OU origem —, medicamento, busca). Medicamento e unidades vem
     * em fetch join (to-one, compativel com paginacao no banco) para evitar N+1. RF-REC-01.
     */
    @Query(value = """
            SELECT r FROM Recomendacao r
              JOIN FETCH r.medicamento m
              JOIN FETCH r.unidadeDestino ud
              LEFT JOIN FETCH r.unidadeOrigem uo
            WHERE (:tipo IS NULL OR r.tipo = :tipo)
              AND (:status IS NULL OR r.status = :status)
              AND (:origemMotor IS NULL OR r.origemMotor = :origemMotor)
              AND (:prioridade IS NULL OR r.prioridade = :prioridade)
              AND (:unidadeId IS NULL OR ud.id = :unidadeId OR uo.id = :unidadeId)
              AND (:medicamentoId IS NULL OR m.id = :medicamentoId)
              AND (:busca IS NULL
                   OR LOWER(m.nome) LIKE LOWER(CONCAT('%', CAST(:busca AS string), '%'))
                   OR LOWER(ud.nome) LIKE LOWER(CONCAT('%', CAST(:busca AS string), '%'))
                   OR LOWER(ud.sigla) LIKE LOWER(CONCAT('%', CAST(:busca AS string), '%'))
                   OR LOWER(r.justificativa) LIKE LOWER(CONCAT('%', CAST(:busca AS string), '%')))
            """,
            countQuery = """
            SELECT COUNT(r) FROM Recomendacao r
              JOIN r.medicamento m
              JOIN r.unidadeDestino ud
              LEFT JOIN r.unidadeOrigem uo
            WHERE (:tipo IS NULL OR r.tipo = :tipo)
              AND (:status IS NULL OR r.status = :status)
              AND (:origemMotor IS NULL OR r.origemMotor = :origemMotor)
              AND (:prioridade IS NULL OR r.prioridade = :prioridade)
              AND (:unidadeId IS NULL OR ud.id = :unidadeId OR uo.id = :unidadeId)
              AND (:medicamentoId IS NULL OR m.id = :medicamentoId)
              AND (:busca IS NULL
                   OR LOWER(m.nome) LIKE LOWER(CONCAT('%', CAST(:busca AS string), '%'))
                   OR LOWER(ud.nome) LIKE LOWER(CONCAT('%', CAST(:busca AS string), '%'))
                   OR LOWER(ud.sigla) LIKE LOWER(CONCAT('%', CAST(:busca AS string), '%'))
                   OR LOWER(r.justificativa) LIKE LOWER(CONCAT('%', CAST(:busca AS string), '%')))
            """)
    Page<Recomendacao> buscarComFiltros(@Param("tipo") TipoRecomendacao tipo,
                                        @Param("status") StatusRecomendacao status,
                                        @Param("origemMotor") OrigemMotor origemMotor,
                                        @Param("prioridade") Prioridade prioridade,
                                        @Param("unidadeId") UUID unidadeId,
                                        @Param("medicamentoId") UUID medicamentoId,
                                        @Param("busca") String busca,
                                        Pageable pageable);

    /** Recomendacao com os relacionamentos carregados (resposta apos aprovar/executar). */
    @Query("""
            SELECT r FROM Recomendacao r
              JOIN FETCH r.medicamento
              JOIN FETCH r.unidadeDestino
              LEFT JOIN FETCH r.unidadeOrigem
            WHERE r.id = :id
            """)
    Optional<Recomendacao> findComRelacionamentos(@Param("id") UUID id);

    // ----- KPIs do painel (RF-REC-01/02/03/05) -----

    long countByStatus(StatusRecomendacao status);

    long countByOrigemMotor(OrigemMotor origemMotor);

    /**
     * Soma a economia estimada das recomendações que ainda contam como potencial — exclui as
     * RECUSADAS (descartadas não geram economia). RF-REC-02.
     */
    @Query("""
            SELECT COALESCE(SUM(r.economiaEstimada), 0) FROM Recomendacao r
            WHERE r.status <> com.alphatech.cahosp.recomendacao.dominio.StatusRecomendacao.RECUSADA
            """)
    BigDecimal somarEconomiaEstimada();

    /**
     * Recomendacoes pendentes de maior economia (destaque do painel), com relacionamentos
     * carregados. RF-DASH-01.
     */
    @Query("""
            SELECT r FROM Recomendacao r
              JOIN FETCH r.medicamento
              JOIN FETCH r.unidadeDestino
              LEFT JOIN FETCH r.unidadeOrigem
            WHERE r.status = :status
            ORDER BY r.economiaEstimada DESC
            """)
    List<Recomendacao> findPendentesPorImpacto(@Param("status") StatusRecomendacao status,
                                               Pageable pageable);

    /**
     * Recomendacoes ainda nao executadas (pendentes ou aprovadas), ordenadas por impacto.
     * RF-DASH-02 / RF-REC-04.
     */
    @Query("""
            SELECT r FROM Recomendacao r
              JOIN FETCH r.medicamento
              JOIN FETCH r.unidadeDestino
              LEFT JOIN FETCH r.unidadeOrigem
            WHERE r.status <> :status
            ORDER BY r.economiaEstimada DESC
            """)
    List<Recomendacao> findAbertas(@Param("status") StatusRecomendacao status, Pageable pageable);

    // ----- Painel filtrado por unidade/medicamento (RF-DASH-01/02) -----

    /**
     * Conta recomendacoes para o painel com filtros opcionais (status, unidade — destino OU
     * origem —, medicamento). Com todos nulos, equivale a contagem da rede inteira.
     */
    @Query("""
            SELECT COUNT(r) FROM Recomendacao r
            WHERE (:status IS NULL OR r.status = :status)
              AND (:origemMotor IS NULL OR r.origemMotor = :origemMotor)
              AND (:unidadeId IS NULL OR r.unidadeDestino.id = :unidadeId OR r.unidadeOrigem.id = :unidadeId)
              AND (:medicamentoId IS NULL OR r.medicamento.id = :medicamentoId)
            """)
    long contarPainel(@Param("status") StatusRecomendacao status,
                      @Param("origemMotor") OrigemMotor origemMotor,
                      @Param("unidadeId") UUID unidadeId,
                      @Param("medicamentoId") UUID medicamentoId);

    /** Variante de {@link #somarEconomiaEstimada} com filtro opcional de unidade/medicamento. */
    @Query("""
            SELECT COALESCE(SUM(r.economiaEstimada), 0) FROM Recomendacao r
            WHERE r.status <> com.alphatech.cahosp.recomendacao.dominio.StatusRecomendacao.RECUSADA
              AND (:unidadeId IS NULL OR r.unidadeDestino.id = :unidadeId OR r.unidadeOrigem.id = :unidadeId)
              AND (:medicamentoId IS NULL OR r.medicamento.id = :medicamentoId)
            """)
    BigDecimal somarEconomiaEstimadaFiltrada(@Param("unidadeId") UUID unidadeId,
                                             @Param("medicamentoId") UUID medicamentoId);

    /** Variante de {@link #findPendentesPorImpacto} com filtro opcional de unidade/medicamento. */
    @Query("""
            SELECT r FROM Recomendacao r
              JOIN FETCH r.medicamento
              JOIN FETCH r.unidadeDestino
              LEFT JOIN FETCH r.unidadeOrigem
            WHERE r.status = :status
              AND (:unidadeId IS NULL OR r.unidadeDestino.id = :unidadeId OR r.unidadeOrigem.id = :unidadeId)
              AND (:medicamentoId IS NULL OR r.medicamento.id = :medicamentoId)
            ORDER BY r.economiaEstimada DESC
            """)
    List<Recomendacao> findPendentesPorImpactoFiltrado(@Param("status") StatusRecomendacao status,
                                                       @Param("unidadeId") UUID unidadeId,
                                                       @Param("medicamentoId") UUID medicamentoId,
                                                       Pageable pageable);

    // ----- Suporte ao motor de geracao -----

    /**
     * Chaves [tipo, medicamentoId, unidadeDestinoId] das recomendacoes existentes — carregadas de
     * uma vez para o motor deduplicar em memoria, em vez de um {@code exists} por candidato (N+1).
     */
    @Query("SELECT r.tipo, r.medicamento.id, r.unidadeDestino.id FROM Recomendacao r")
    List<Object[]> chavesExistentes();

    /**
     * Remove as recomendacoes pendentes (renova as nao tratadas na regeneracao). Delete derivado
     * (carrega e remove), preservando aprovadas/executadas.
     */
    long deleteByStatus(StatusRecomendacao status);
}
