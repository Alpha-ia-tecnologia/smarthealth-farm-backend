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
     * Lista previsoes, paginada, com filtros opcionais; insumo e unidade vem em fetch join
     * (to-one, compativel com paginacao no banco) para evitar N+1 ao montar a resposta
     * (criticidade, nomes, siglas). RF-PRV-01.
     */
    @Query(value = """
            SELECT p FROM Previsao p
              JOIN FETCH p.insumo m
              JOIN FETCH p.unidade u
            WHERE (:unidadeId IS NULL OR u.id = :unidadeId)
              AND (:insumoId IS NULL OR m.id = :insumoId)
              AND (:drift IS NULL OR p.drift = :drift)
              AND (:busca IS NULL
                   OR LOWER(m.nome) LIKE LOWER(CONCAT('%', CAST(:busca AS string), '%'))
                   OR LOWER(u.nome) LIKE LOWER(CONCAT('%', CAST(:busca AS string), '%'))
                   OR LOWER(u.sigla) LIKE LOWER(CONCAT('%', CAST(:busca AS string), '%')))
            """,
            countQuery = """
            SELECT COUNT(p) FROM Previsao p
              JOIN p.insumo m
              JOIN p.unidade u
            WHERE (:unidadeId IS NULL OR u.id = :unidadeId)
              AND (:insumoId IS NULL OR m.id = :insumoId)
              AND (:drift IS NULL OR p.drift = :drift)
              AND (:busca IS NULL
                   OR LOWER(m.nome) LIKE LOWER(CONCAT('%', CAST(:busca AS string), '%'))
                   OR LOWER(u.nome) LIKE LOWER(CONCAT('%', CAST(:busca AS string), '%'))
                   OR LOWER(u.sigla) LIKE LOWER(CONCAT('%', CAST(:busca AS string), '%')))
            """)
    Page<Previsao> buscarComFiltros(@Param("unidadeId") UUID unidadeId,
                                    @Param("insumoId") UUID insumoId,
                                    @Param("drift") Drift drift,
                                    @Param("busca") String busca,
                                    Pageable pageable);

    /** Todas as previsoes com o insumo carregado (para os KPIs do painel). RF-PRV-05/06. */
    @Query("SELECT p FROM Previsao p JOIN FETCH p.insumo")
    List<Previsao> findTodasComInsumo();

    /** Variante de {@link #findTodasComInsumo} com filtro opcional de unidade/insumo (resumo filtrado). */
    @Query("""
            SELECT p FROM Previsao p
              JOIN FETCH p.insumo
            WHERE (:unidadeId IS NULL OR p.unidade.id = :unidadeId)
              AND (:insumoId IS NULL OR p.insumo.id = :insumoId)
            """)
    List<Previsao> findFiltradasComInsumo(@Param("unidadeId") UUID unidadeId,
                                               @Param("insumoId") UUID insumoId);

    /**
     * Serie agregada (soma por periodo) de um insumo — base do grafico consolidado do
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
             WHERE ps.previsao.insumo.id = :insumoId
               AND (:unidadeId IS NULL OR ps.previsao.unidade.id = :unidadeId)
             GROUP BY ps.periodo, ps.ordem
             ORDER BY ps.ordem
            """)
    List<SeriePeriodoAgregada> agregarSeriePorInsumo(@Param("insumoId") UUID insumoId,
                                                          @Param("unidadeId") UUID unidadeId);

    /** Previsao com a serie temporal carregada (drill-down/grafico). RF-PRV-02. */
    @Query("""
            SELECT DISTINCT p FROM Previsao p
              JOIN FETCH p.insumo
              JOIN FETCH p.unidade
              LEFT JOIN FETCH p.serie
            WHERE p.insumo.id = :insumoId AND p.unidade.id = :unidadeId
            """)
    Optional<Previsao> findDetalhe(@Param("insumoId") UUID insumoId,
                                   @Param("unidadeId") UUID unidadeId);
}
