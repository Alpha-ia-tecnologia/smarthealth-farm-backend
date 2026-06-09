package com.alphatech.cahosp.previsao;

import com.alphatech.cahosp.previsao.dominio.Drift;
import com.alphatech.cahosp.previsao.dominio.Previsao;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PrevisaoRepository extends JpaRepository<Previsao, UUID> {

    /**
     * Lista previsoes com filtros opcionais; medicamento e unidade vem em fetch join para
     * evitar N+1 ao montar a resposta (criticidade, nomes, siglas). RF-PRV-01.
     */
    @Query("""
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
            """)
    List<Previsao> buscarComFiltros(@Param("unidadeId") UUID unidadeId,
                                    @Param("medicamentoId") UUID medicamentoId,
                                    @Param("drift") Drift drift,
                                    @Param("busca") String busca,
                                    Sort sort);

    /** Todas as previsoes com o medicamento carregado (para os KPIs do painel). RF-PRV-05/06. */
    @Query("SELECT p FROM Previsao p JOIN FETCH p.medicamento")
    List<Previsao> findTodasComMedicamento();

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
