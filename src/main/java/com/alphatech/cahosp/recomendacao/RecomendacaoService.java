package com.alphatech.cahosp.recomendacao;

import com.alphatech.cahosp.comum.excecao.RecursoNaoEncontradoException;
import com.alphatech.cahosp.recomendacao.dominio.OrigemMotor;
import com.alphatech.cahosp.recomendacao.dominio.Prioridade;
import com.alphatech.cahosp.recomendacao.dominio.Recomendacao;
import com.alphatech.cahosp.recomendacao.dominio.StatusRecomendacao;
import com.alphatech.cahosp.recomendacao.dominio.TipoRecomendacao;
import com.alphatech.cahosp.recomendacao.dto.GeracaoRecomendacoesResponse;
import com.alphatech.cahosp.recomendacao.dto.RecomendacaoResponse;
import com.alphatech.cahosp.recomendacao.dto.ResumoRecomendacoesResponse;
import com.alphatech.cahosp.seguranca.auditoria.RegistradorAuditoria;
import com.alphatech.cahosp.seguranca.auditoria.dominio.CategoriaAuditoria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

/**
 * Regra de negocio das recomendacoes (RF-REC): consulta com filtros, KPIs do painel, aprovacao/
 * execucao (acoes de Gestor) e disparo do motor de geracao.
 */
@Service
@Transactional(readOnly = true)
public class RecomendacaoService {

    private final RecomendacaoRepository recomendacaoRepository;
    private final GeradorRecomendacao geradorRecomendacao;
    private final RegistradorAuditoria auditoria;

    public RecomendacaoService(RecomendacaoRepository recomendacaoRepository,
                               GeradorRecomendacao geradorRecomendacao,
                               RegistradorAuditoria auditoria) {
        this.recomendacaoRepository = recomendacaoRepository;
        this.geradorRecomendacao = geradorRecomendacao;
        this.auditoria = auditoria;
    }

    /**
     * Lista recomendacoes, paginada, com filtros opcionais. A ordenacao default (maior economia
     * primeiro) vem do controller. RF-REC-01.
     */
    public Page<RecomendacaoResponse> listar(TipoRecomendacao tipo, StatusRecomendacao status,
                                             OrigemMotor origemMotor, Prioridade prioridade,
                                             UUID unidadeId, UUID medicamentoId, String busca,
                                             Pageable pageable) {
        String termo = (busca == null || busca.isBlank()) ? null : busca.trim();
        return recomendacaoRepository
                .buscarComFiltros(tipo, status, origemMotor, prioridade, unidadeId, medicamentoId,
                        termo, pageable)
                .map(RecomendacaoResponse::de);
    }

    /** KPIs do painel de recomendacoes (RF-REC-01/02/03/05). */
    public ResumoRecomendacoesResponse resumo() {
        long total = recomendacaoRepository.count();
        long pendentes = recomendacaoRepository.countByStatus(StatusRecomendacao.PENDENTE);
        long aprovadas = recomendacaoRepository.countByStatus(StatusRecomendacao.APROVADA);
        long executadas = recomendacaoRepository.countByStatus(StatusRecomendacao.EXECUTADA);
        long aderiram = recomendacaoRepository.countByStatusNot(StatusRecomendacao.PENDENTE);
        long geradasPorIA = recomendacaoRepository.countByOrigemMotor(OrigemMotor.APRENDIZADO_MAQUINA);
        BigDecimal economia = recomendacaoRepository.somarEconomiaEstimada()
                .setScale(2, RoundingMode.HALF_UP);
        int taxaAdesao = total == 0 ? 0 : Math.round((aderiram * 100f) / total);
        return new ResumoRecomendacoesResponse(
                pendentes, aprovadas, executadas, economia, geradasPorIA, taxaAdesao, total);
    }

    /** Aprova uma recomendacao pendente (RF-REC — acao de Gestor). */
    @Transactional
    public RecomendacaoResponse aprovar(UUID id) {
        Recomendacao recomendacao = carregar(id);
        recomendacao.aprovar();
        RecomendacaoResponse resposta = RecomendacaoResponse.de(recomendacaoRepository.save(recomendacao));
        auditoria.registrar(CategoriaAuditoria.APROVAR_RECOMENDACAO, "recomendacao:" + id);
        return resposta;
    }

    /** Marca uma recomendacao aprovada como executada (RF-REC-05 — acao de Gestor). */
    @Transactional
    public RecomendacaoResponse executar(UUID id) {
        Recomendacao recomendacao = carregar(id);
        recomendacao.executar();
        RecomendacaoResponse resposta = RecomendacaoResponse.de(recomendacaoRepository.save(recomendacao));
        auditoria.registrar(CategoriaAuditoria.EXECUTAR_RECOMENDACAO, "recomendacao:" + id);
        return resposta;
    }

    /** Dispara o motor de geracao por regra (RF-REC-01 — acao de Gestor). */
    @Transactional
    public GeracaoRecomendacoesResponse gerar() {
        return geradorRecomendacao.gerar();
    }

    private Recomendacao carregar(UUID id) {
        return recomendacaoRepository.findComRelacionamentos(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Recomendacao nao encontrada: " + id + "."));
    }
}
