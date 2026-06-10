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
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Regra de negocio dos alertas (RF-ALE): consulta com filtros, KPIs do painel, tratamento
 * (transicao de status) e disparo do motor de geracao.
 */
@Service
@Transactional(readOnly = true)
public class AlertaService {

    /**
     * Ordenacao: mais urgentes primeiro (menor cobertura/prazo). Espelha o
     * {@code alertas.sort((a, b) => diasParaEvento)} do front.
     */
    private static final Sort ORDEM_URGENCIA = Sort.by("diasParaEvento").ascending();

    private final AlertaRepository alertaRepository;
    private final GeradorAlerta geradorAlerta;
    private final RegistradorAuditoria auditoria;

    public AlertaService(AlertaRepository alertaRepository, GeradorAlerta geradorAlerta,
                         RegistradorAuditoria auditoria) {
        this.alertaRepository = alertaRepository;
        this.geradorAlerta = geradorAlerta;
        this.auditoria = auditoria;
    }

    /** Lista alertas com filtros opcionais (tipo, severidade, status, unidade, medicamento, busca). */
    public List<AlertaResponse> listar(TipoAlerta tipo, Severidade severidade, StatusAlerta status,
                                       UUID unidadeId, UUID medicamentoId, String busca) {
        String termo = (busca == null || busca.isBlank()) ? null : busca.trim();
        return alertaRepository
                .buscarComFiltros(tipo, severidade, status, unidadeId, medicamentoId, termo, ORDEM_URGENCIA)
                .stream()
                .map(AlertaResponse::de)
                .toList();
    }

    /** KPIs do painel de alertas (RF-ALE-04/05). */
    public ResumoAlertasResponse resumo() {
        long abertos = alertaRepository.countByStatus(StatusAlerta.ABERTO);
        long desabastecimento = alertaRepository.countByTipoAndStatusNot(
                TipoAlerta.DESABASTECIMENTO, StatusAlerta.RESOLVIDO);
        long vencimento = alertaRepository.countByTipoAndStatusNot(
                TipoAlerta.VENCIMENTO, StatusAlerta.RESOLVIDO);
        long criticos = alertaRepository.countBySeveridadeAndStatusNot(
                Severidade.CRITICO, StatusAlerta.RESOLVIDO);
        long emTratamento = alertaRepository.countByStatus(StatusAlerta.EM_TRATAMENTO);
        long resolvidos = alertaRepository.countByStatus(StatusAlerta.RESOLVIDO);
        long total = alertaRepository.count();
        return new ResumoAlertasResponse(
                abertos, desabastecimento, vencimento, criticos, emTratamento, resolvidos, total);
    }

    /** Aplica uma transicao de status no tratamento de um alerta (RF-ALE-05). */
    @Transactional
    public AlertaResponse atualizarStatus(UUID id, StatusAlerta novoStatus) {
        Alerta alerta = alertaRepository.findComRelacionamentos(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Alerta nao encontrado: " + id + "."));
        alerta.mudarStatusPara(novoStatus);
        return AlertaResponse.de(alertaRepository.save(alerta));
    }

    /** Dispara o motor de geracao por regra (RF-ALE-01/02 — acao de Gestor). */
    @Transactional
    public GeracaoAlertasResponse gerar() {
        GeracaoAlertasResponse resultado = geradorAlerta.gerar(LocalDate.now());
        auditoria.registrar(CategoriaAuditoria.GERAR_ALERTAS, "alertas:motor-regras");
        return resultado;
    }
}
