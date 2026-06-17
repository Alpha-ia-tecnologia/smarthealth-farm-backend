package com.alphatech.cahosp.previsao;

import com.alphatech.cahosp.previsao.dominio.Drift;
import com.alphatech.cahosp.previsao.dominio.Previsao;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PrevisaoRepository extends JpaRepository<Previsao, UUID> {

    /**
     * Lista previsoes, paginada, com filtros opcionais; medicamento e unidade vem em fetch join
     * (to-one, compativel com paginacao no banco) para evitar N+1 ao montar a resposta
     * (criticidade, nomes, siglas). RF-PRV-01.
     */
    @Query(value = """
            SELECT p FROM Previsao p
              JOIN FETCH p.medicamento m
              JOIN FETCH p.unidade u
            WHERE (:unidadeId IS NULL OR u.id = :unidadeId)
              AND (:medicamentoId IS NULL OR m.id = :medicamentoId)
              AND (:drift IS NULL OR p.drift = :drift)
              AND (:busca IS NULL
                   OR LOWER(m.nome) LIKE LOWER(CONCAT('%', CAST(:busca AS string), '%'))
                   OR LOWER(u.nome) LIKE LOWER(CONCAT('%', CAST(:busca AS string), '%'))
                   OR LOWER(u.sigla) LIKE LOWER(CONCAT('%', CAST(:busca AS string), '%')))
            """,
            countQuery = """
            SELECT COUNT(p) FROM Previsao p
              JOIN p.medicamento m
              JOIN p.unidade u
            WHERE (:unidadeId IS NULL OR u.id = :unidadeId)
              AND (:medicamentoId IS NULL OR m.id = :medicamentoId)
              AND (:drift IS NULL OR p.drift = :drift)
              AND (:busca IS NULL
                   OR LOWER(m.nome) LIKE LOWER(CONCAT('%', CAST(:busca AS string), '%'))
                   OR LOWER(u.nome) LIKE LOWER(CONCAT('%', CAST(:busca AS string), '%'))
                   OR LOWER(u.sigla) LIKE LOWER(CONCAT('%', CAST(:busca AS string), '%')))
            """)
    Page<Previsao> buscarComFiltros(@Param("unidadeId") UUID unidadeId,
                                    @Param("medicamentoId") UUID medicamentoId,
                                    @Param("drift") Drift drift,
                                    @Param("busca") String busca,
                                    Pageable pageable);

    /** Todas as previsoes com o medicamento carregado (para os KPIs do painel). RF-PRV-05/06. */
    @Query("SELECT p FROM Previsao p JOIN FETCH p.medicamento")
    List<Previsao> findTodasComMedicamento();

    /** Variante de {@link #findTodasComMedicamento} com filtro opcional de unidade/medicamento (resumo filtrado). */
    @Query("""
            SELECT p FROM Previsao p
              JOIN FETCH p.medicamento
            WHERE (:unidadeId IS NULL OR p.unidade.id = :unidadeId)
              AND (:medicamentoId IS NULL OR p.medicamento.id = :medicamentoId)
            """)
    List<Previsao> findFiltradasComMedicamento(@Param("unidadeId") UUID unidadeId,
                                               @Param("medicamentoId") UUID medicamentoId);

    /**
     * Serie agregada (soma por periodo) de um medicamento — base do grafico consolidado do
     * dashboard (RF-DASH/RF-PRV-02). O filtro opcional {@code unidadeId} restringe a serie a uma
     * unidade (dashboard filtrado); nulo soma todas as unidades. Ordenada cronologicamente.
     */
    @Query("""
            SELECT ps.periodo AS periodo,
                   SUM(ps.realizado) AS realizado,
                   SUM(ps.previsto) AS previsto,
                   SUM(ps.limiteInferior) AS limiteInferior,
                   SUM(ps.limiteSuperior) AS limiteSuperior
              FROM PontoSerie ps
             WHERE ps.previsao.medicamento.id = :medicamentoId
               AND (:unidadeId IS NULL OR ps.previsao.unidade.id = :unidadeId)
             GROUP BY ps.periodo, ps.ordem
             ORDER BY ps.ordem
            """)
    List<SeriePeriodoAgregada> agregarSeriePorMedicamento(@Param("medicamentoId") UUID medicamentoId,
                                                          @Param("unidadeId") UUID unidadeId);

    /** Previsao com a serie temporal carregada (drill-down/grafico). RF-PRV-02. */
    @Query("""
            SELECT DISTINCT p FROM Previsao p
              JOIN FETCH p.medicamento
              JOIN FETCH p.unidade
              LEFT JOIN FETCH p.serie
            WHERE p.medicamento.id = :medicamentoId AND p.unidade.id = :unidadeId
            """)
    Optional<Previsao> findDetalhe(@Param("medicamentoId") UUID medicamentoId,
                                   @Param("unidadeId") UUID unidadeId);
}
