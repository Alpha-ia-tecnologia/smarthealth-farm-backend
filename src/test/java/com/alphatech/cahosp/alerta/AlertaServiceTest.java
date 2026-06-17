package com.alphatech.cahosp.alerta;

import com.alphatech.cahosp.alerta.dominio.Alerta;
import com.alphatech.cahosp.alerta.dominio.Severidade;
import com.alphatech.cahosp.alerta.dominio.StatusAlerta;
import com.alphatech.cahosp.alerta.dominio.TipoAlerta;
import com.alphatech.cahosp.insumo.dominio.Insumo;
import com.alphatech.cahosp.seguranca.auditoria.RegistradorAuditoria;
import com.alphatech.cahosp.seguranca.auditoria.dominio.CategoriaAuditoria;
import com.alphatech.cahosp.unidade.dominio.Unidade;
import com.alphatech.cahosp.usuario.dominio.Perfil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testes unitarios da regra de tratamento de alertas (RF-ALE-05 + RF-SEG-02): cada transicao de
 * status efetiva e registrada na trilha de auditoria (quem tratou/resolveu), e a transicao
 * idempotente (mesmo status) nao gera registro.
 */
@ExtendWith(MockitoExtension.class)
class AlertaServiceTest {

    @Mock private AlertaRepository alertaRepository;
    @Mock private GeradorAlerta geradorAlerta;
    @Mock private RegistradorAuditoria auditoria;
    @InjectMocks private AlertaService service;

    /** Alerta real (transicoes de status reais) com relacionamentos mockados (so para o DTO). */
    private Alerta alertaAberto() {
        return new Alerta(TipoAlerta.DESABASTECIMENTO, Severidade.CRITICO,
                org.mockito.Mockito.mock(Insumo.class), org.mockito.Mockito.mock(Unidade.class),
                null, "Cobertura abaixo do minimo.", Set.of(Perfil.OPERADOR), 3);
    }

    @Test
    @DisplayName("Marcar como Em tratamento audita TRATAR_ALERTA com a acao e o recurso corretos")
    void auditaInicioDeTratamento() {
        UUID id = UUID.randomUUID();
        Alerta alerta = alertaAberto();
        when(alertaRepository.findComRelacionamentos(id)).thenReturn(Optional.of(alerta));
        when(alertaRepository.save(any(Alerta.class))).thenAnswer(inv -> inv.getArgument(0));

        service.atualizarStatus(id, StatusAlerta.EM_TRATAMENTO);

        verify(auditoria).registrar(
                eq(CategoriaAuditoria.TRATAR_ALERTA),
                eq("Marcou alerta como Em tratamento"),
                eq("alerta:" + id),
                anyString(),
                eq(false));
    }

    @Test
    @DisplayName("Resolver audita TRATAR_ALERTA com a acao 'Resolveu alerta'")
    void auditaResolucao() {
        UUID id = UUID.randomUUID();
        Alerta alerta = alertaAberto();
        when(alertaRepository.findComRelacionamentos(id)).thenReturn(Optional.of(alerta));
        when(alertaRepository.save(any(Alerta.class))).thenAnswer(inv -> inv.getArgument(0));

        service.atualizarStatus(id, StatusAlerta.RESOLVIDO);

        verify(auditoria).registrar(
                eq(CategoriaAuditoria.TRATAR_ALERTA),
                eq("Resolveu alerta"),
                eq("alerta:" + id),
                anyString(),
                eq(false));
    }

    @Test
    @DisplayName("Transicao para o mesmo status e idempotente: nao audita de novo")
    void naoAuditaTransicaoIdempotente() {
        UUID id = UUID.randomUUID();
        Alerta alerta = alertaAberto();
        when(alertaRepository.findComRelacionamentos(id)).thenReturn(Optional.of(alerta));
        when(alertaRepository.save(any(Alerta.class))).thenAnswer(inv -> inv.getArgument(0));

        service.atualizarStatus(id, StatusAlerta.EM_TRATAMENTO); // muda de fato (audita)
        service.atualizarStatus(id, StatusAlerta.EM_TRATAMENTO); // sem mudanca (nao audita)

        verify(auditoria, times(1)).registrar(any(), anyString(), anyString(), anyString(), eq(false));
        verify(auditoria, never()).registrar(any(), anyString());
    }
}
