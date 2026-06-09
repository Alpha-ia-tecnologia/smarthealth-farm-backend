package com.alphatech.cahosp.recomendacao;

import com.alphatech.cahosp.recomendacao.dominio.OrigemMotor;
import com.alphatech.cahosp.recomendacao.dominio.Prioridade;
import com.alphatech.cahosp.recomendacao.dominio.Recomendacao;
import com.alphatech.cahosp.recomendacao.dominio.StatusRecomendacao;
import com.alphatech.cahosp.recomendacao.dominio.TipoRecomendacao;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RecomendacaoRepository extends JpaRepository<Recomendacao, UUID> {

    /**
     * Lista recomendacoes com filtros opcionais (tipo, status, origem do motor, prioridade,
     * unidade — destino OU origem —, medicamento, busca). Medicamento e unidades vem em fetch
     * join para evitar N+1 ao montar a resposta. RF-REC-01.
     */
    @Query("""
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
            """)
    List<Recomendacao> buscarComFiltros(@Param("tipo") TipoRecomendacao tipo,
                                        @Param("status") StatusRecomendacao status,
                                        @Param("origemMotor") OrigemMotor origemMotor,
                                        @Param("prioridade") Prioridade prioridade,
                                        @Param("unidadeId") UUID unidadeId,
                                        @Param("medicamentoId") UUID medicamentoId,
                                        @Param("busca") String busca,
                                        Sort sort);

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

    long countByStatusNot(StatusRecomendacao status);

    long countByOrigemMotor(OrigemMotor origemMotor);

    @Query("SELECT COALESCE(SUM(r.economiaEstimada), 0) FROM Recomendacao r")
    BigDecimal somarEconomiaEstimada();

    // ----- Suporte ao motor de geracao -----

    /** Ja existe recomendacao deste tipo para o medicamento/unidade destino (qualquer status)? */
    boolean existsByTipoAndMedicamentoIdAndUnidadeDestinoId(
            TipoRecomendacao tipo, UUID medicamentoId, UUID unidadeDestinoId);

    /**
     * Remove as recomendacoes pendentes (renova as nao tratadas na regeneracao). Delete derivado
     * (carrega e remove), preservando aprovadas/executadas.
     */
    long deleteByStatus(StatusRecomendacao status);
}
