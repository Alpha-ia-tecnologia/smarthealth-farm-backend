package com.alphatech.cahosp.seguranca.auditoria;

import com.alphatech.cahosp.seguranca.auditoria.dominio.LogAuditoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

/**
 * Persistencia da trilha de auditoria (RF-SEG-02). O filtro dinamico da listagem usa
 * {@link JpaSpecificationExecutor} (Criteria API) — evita o {@code :param IS NULL} de tipo
 * indeterminado no PostgreSQL para filtros opcionais (em especial o boolean {@code assistidoPorIa}).
 */
public interface LogAuditoriaRepository
        extends JpaRepository<LogAuditoria, UUID>, JpaSpecificationExecutor<LogAuditoria> {

    /** KPI: decisoes assistidas por IA (RF-SEG-02/04). */
    long countByAssistidoPorIaTrue();

    /** KPI: eventos com base legal LGPD registrada (RF-SEG-03). */
    long countByBaseLegalIsNotNull();

    /** Evento mais recente da trilha (ultima atividade do painel). */
    Optional<LogAuditoria> findTopByOrderByDataDesc();
}
