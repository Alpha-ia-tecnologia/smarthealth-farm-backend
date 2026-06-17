package com.alphatech.cahosp.alerta;

import com.alphatech.cahosp.alerta.dominio.Alerta;
import com.alphatech.cahosp.alerta.dominio.Severidade;
import com.alphatech.cahosp.alerta.dominio.StatusAlerta;
import com.alphatech.cahosp.alerta.dominio.TipoAlerta;
import com.alphatech.cahosp.alerta.dto.AlertaResponse;
import com.alphatech.cahosp.alerta.dto.GeracaoAlertasResponse;
import com.alphatech.cahosp.alerta.dto.ResumoAlertasResponse;
import com.alphatech.cahosp.comum.excecao.RecursoNaoEncontradoException;
import com.alphatech.cahosp.seguranca.auditoria.RegistradorAuditoria;
import com.alphatech.cahosp.seguranca.auditoria.dominio.CategoriaAuditoria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Regra de negocio dos alertas (RF-ALE): consulta paginada com filtros, KPIs do painel,
 * tratamento (transicao de status) e disparo do motor de geracao.
 */
@Service
@Transactional(readOnly = true)
public class AlertaService {

    private final AlertaRepository alertaRepository;
    private final GeradorAlerta geradorAlerta;
    private final RegistradorAuditoria auditoria;

    public AlertaService(AlertaRepository alertaRepository, GeradorAlerta geradorAlerta,
                         RegistradorAuditoria auditoria) {
        this.alertaRepository = alertaRepository;
        this.geradorAlerta = geradorAlerta;
        this.auditoria = auditoria;
    }

    /**
     * Lista alertas, paginada, com filtros opcionais (tipo, severidade, status, unidade,
     * medicamento, busca). A ordenacao default (mais urgentes primeiro) vem do controller.
     */
    public Page<AlertaResponse> listar(TipoAlerta tipo, Severidade severidade, StatusAlerta status,
                                       UUID unidadeId, UUID medicamentoId, String busca,
                                       Pageable pageable) {
        String termo = (busca == null || busca.isBlank()) ? null : busca.trim();
        return alertaRepository
                .buscarComFiltros(tipo, severidade, status, unidadeId, medicamentoId, termo, pageable)
                .map(AlertaResponse::de);
    }

    /** KPIs do painel de alertas, com filtros opcionais de unidade/medicamento (RF-ALE-04/05). */
    public ResumoAlertasResponse resumo(UUID unidadeId, UUID medicamentoId) {
        long abertos = alertaRepository.contarPainel(null, null, StatusAlerta.ABERTO, null, unidadeId, medicamentoId);
        long desabastecimento = alertaRepository.contarPainel(
                TipoAlerta.DESABASTECIMENTO, null, null, StatusAlerta.RESOLVIDO, unidadeId, medicamentoId);
        long vencimento = alertaRepository.contarPainel(
                TipoAlerta.VENCIMENTO, null, null, StatusAlerta.RESOLVIDO, unidadeId, medicamentoId);
        long criticos = alertaRepository.contarPainel(
                null, Severidade.CRITICO, null, StatusAlerta.RESOLVIDO, unidadeId, medicamentoId);
        long emTratamento = alertaRepository.contarPainel(null, null, StatusAlerta.EM_TRATAMENTO, null, unidadeId, medicamentoId);
        long resolvidos = alertaRepository.contarPainel(null, null, StatusAlerta.RESOLVIDO, null, unidadeId, medicamentoId);
        long total = alertaRepository.contarPainel(null, null, null, null, unidadeId, medicamentoId);
        // "Ativos" = não resolvidos: é o card principal e fecha com os cards por tipo
        // (desabastecimento + vencimento == ativos) e com o todo (ativos + resolvidos == total).
        long ativos = abertos + emTratamento;
        return new ResumoAlertasResponse(
                ativos, abertos, emTratamento, desabastecimento, vencimento, criticos, resolvidos, total);
    }

    /**
     * Aplica uma transicao de status no tratamento de um alerta (RF-ALE-05) e registra a acao na
     * trilha de auditoria (RF-SEG-02): fica gravado <em>quem</em> tratou/resolveu o alerta e quando.
     * So audita quando ha mudanca efetiva (a transicao para o mesmo status e idempotente).
     */
    @Transactional
    public AlertaResponse atualizarStatus(UUID id, StatusAlerta novoStatus) {
        Alerta alerta = alertaRepository.findComRelacionamentos(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Alerta nao encontrado: " + id + "."));
        StatusAlerta anterior = alerta.getStatus();
        alerta.mudarStatusPara(novoStatus);
        AlertaResponse resposta = AlertaResponse.de(alertaRepository.save(alerta));
        if (novoStatus != anterior) {
            auditoria.registrar(
                    CategoriaAuditoria.TRATAR_ALERTA,
                    acaoTratamento(novoStatus),
                    "alerta:" + id,
                    CategoriaAuditoria.TRATAR_ALERTA.baseLegalPadrao(),
                    false);
        }
        return resposta;
    }

    /** Frase legivel da acao de tratamento, conforme o status alcancado (RF-ALE-05). */
    private static String acaoTratamento(StatusAlerta novoStatus) {
        return novoStatus == StatusAlerta.RESOLVIDO
                ? "Resolveu alerta"
                : "Marcou alerta como " + novoStatus.rotulo();
    }

    /** Dispara o motor de geracao por regra (RF-ALE-01/02 — acao de Gestor). */
    @Transactional
    public GeracaoAlertasResponse gerar() {
        GeracaoAlertasResponse resultado = geradorAlerta.gerar(LocalDate.now());
        auditoria.registrar(CategoriaAuditoria.GERAR_ALERTAS, "alertas:motor-regras");
        return resultado;
    }
}
