package com.alphatech.cahosp.alerta;

import com.alphatech.cahosp.alerta.dominio.LimiarAlerta;
import com.alphatech.cahosp.alerta.dto.AtualizarLimiarAlertaRequest;
import com.alphatech.cahosp.alerta.dto.LimiarAlertaResponse;
import com.alphatech.cahosp.seguranca.auditoria.RegistradorAuditoria;
import com.alphatech.cahosp.seguranca.auditoria.dominio.CategoriaAuditoria;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Configuracao dos limiares de disparo (RF-ALE-03). Leitura para qualquer autenticado; alteracao
 * e decisao de negocio (Gestor, garantido no controller) e entra na trilha de auditoria.
 */
@Service
@Transactional(readOnly = true)
public class LimiarAlertaService {

    private final LimiarAlertaRepository limiarRepository;
    private final RegistradorAuditoria auditoria;

    public LimiarAlertaService(LimiarAlertaRepository limiarRepository,
                               RegistradorAuditoria auditoria) {
        this.limiarRepository = limiarRepository;
        this.auditoria = auditoria;
    }

    /** Configuracao vigente (linha singleton criada na migration V12). */
    public LimiarAlertaResponse buscar() {
        return LimiarAlertaResponse.de(configuracao());
    }

    /** Atualiza os limiares (RF-ALE-03) e registra a alteracao na auditoria (RF-SEG). */
    @Transactional
    public LimiarAlertaResponse atualizar(AtualizarLimiarAlertaRequest request) {
        LimiarAlerta limiares = configuracao();
        limiares.atualizar(
                request.percentualEstoqueMinimo(),
                request.coberturaCriticaDias(),
                request.coberturaAltaDias(),
                request.antecedenciaVencimentoDias(),
                request.vencimentoCriticoDias(),
                request.vencimentoAltoDias(),
                request.desabastecimentoAtivo(),
                request.vencimentoAtivo());
        LimiarAlerta salvo = limiarRepository.save(limiares);
        auditoria.registrar(CategoriaAuditoria.ALTERAR_LIMIAR_ALERTA, "alertas:limiares");
        return LimiarAlertaResponse.de(salvo);
    }

    /** A linha singleton existe por migration; ausencia e estado inconsistente do banco. */
    LimiarAlerta configuracao() {
        return limiarRepository.findById(LimiarAlerta.ID_CONFIG)
                .orElseThrow(() -> new IllegalStateException(
                        "Configuracao de limiares ausente — a migration V12 deveria te-la criado."));
    }
}
