package com.alphatech.cahosp.seguranca.auditoria;

import com.alphatech.cahosp.seguranca.auditoria.dominio.CategoriaAuditoria;
import com.alphatech.cahosp.seguranca.auditoria.dominio.LogAuditoria;
import com.alphatech.cahosp.seguranca.auditoria.dto.LogAuditoriaResponse;
import com.alphatech.cahosp.seguranca.auditoria.dto.ResumoAuditoriaResponse;
import com.alphatech.cahosp.usuario.dominio.Perfil;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Regra de negocio da auditoria (RF-SEG). Concentra a area de auditoria: <strong>registra</strong>
 * acoes sensiveis (implementando {@link RegistradorAuditoria}, a porta usada pelos demais dominios)
 * e <strong>consulta</strong> a trilha para a governanca (somente leitura).
 *
 * <p>O registro resolve o ator (usuario + IP) pelo {@link ContextoAuditoria}, mantendo os
 * chamadores ignorantes de seguranca/persistencia (DIP). Roda na transacao do chamador (propagacao
 * padrao): se a acao de negocio for revertida, o log nao e gravado — a trilha reflete so o que de
 * fato ocorreu.
 */
@Service
@Transactional(readOnly = true)
public class AuditoriaService implements RegistradorAuditoria {

    /** Mais recentes primeiro — ordem natural da revisao de conformidade. */
    private static final Sort ORDEM_RECENTE = Sort.by("data").descending();

    private final LogAuditoriaRepository repository;
    private final ContextoAuditoria contexto;

    public AuditoriaService(LogAuditoriaRepository repository, ContextoAuditoria contexto) {
        this.repository = repository;
        this.contexto = contexto;
    }

    @Override
    @Transactional
    public void registrar(CategoriaAuditoria categoria, String recurso) {
        registrar(categoria, categoria.acaoPadrao(), recurso,
                categoria.baseLegalPadrao(), categoria.assistidoPorIaPadrao());
    }

    @Override
    @Transactional
    public void registrar(CategoriaAuditoria categoria, String acao, String recurso,
                          String baseLegal, boolean assistidoPorIa) {
        AtorAuditoria ator = contexto.atorAtual();
        repository.save(new LogAuditoria(
                Instant.now(),
                ator.usuarioId(),
                ator.nome(),
                ator.perfil(),
                categoria,
                acao,
                recurso,
                baseLegal,
                assistidoPorIa,
                ator.ip()));
    }

    /** Lista a trilha com filtros opcionais (categoria, perfil, IA, busca), mais recentes primeiro. */
    public List<LogAuditoriaResponse> listar(CategoriaAuditoria categoria, Perfil perfil,
                                             Boolean assistidoPorIa, String busca) {
        return repository
                .findAll(EspecificacoesAuditoria.comFiltros(categoria, perfil, assistidoPorIa, busca),
                        ORDEM_RECENTE)
                .stream()
                .map(LogAuditoriaResponse::de)
                .toList();
    }

    /** KPIs do painel de auditoria (RF-SEG-02/03/05). */
    public ResumoAuditoriaResponse resumo() {
        return new ResumoAuditoriaResponse(
                repository.count(),
                repository.countByAssistidoPorIaTrue(),
                repository.countByBaseLegalIsNotNull(),
                repository.findTopByOrderByDataDesc()
                        .map(LogAuditoria::getData)
                        .orElse(null));
    }
}
