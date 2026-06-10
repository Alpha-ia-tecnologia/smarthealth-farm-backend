package com.alphatech.cahosp.seguranca.auditoria;

import com.alphatech.cahosp.seguranca.auditoria.dominio.CategoriaAuditoria;
import com.alphatech.cahosp.seguranca.auditoria.dominio.LogAuditoria;
import com.alphatech.cahosp.seguranca.auditoria.dto.ResumoAuditoriaResponse;
import com.alphatech.cahosp.usuario.dominio.Perfil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testes unitarios da regra de auditoria (RF-SEG): o registro copia o ator resolvido pelo contexto,
 * aplica os valores-padrao da categoria e persiste; o resumo agrega os KPIs do painel.
 */
@ExtendWith(MockitoExtension.class)
class AuditoriaServiceTest {

    @Mock private LogAuditoriaRepository repository;
    @Mock private ContextoAuditoria contexto;
    @InjectMocks private AuditoriaService service;

    @Test
    @DisplayName("registrar(categoria, recurso) usa os padroes da categoria e o ator do contexto")
    void registrarComPadroes() {
        UUID usuarioId = UUID.randomUUID();
        when(contexto.atorAtual())
                .thenReturn(new AtorAuditoria(usuarioId, "Ana Sousa", Perfil.GESTOR, "10.0.0.9"));

        service.registrar(CategoriaAuditoria.APROVAR_RECOMENDACAO, "recomendacao:rc-1");

        ArgumentCaptor<LogAuditoria> captor = ArgumentCaptor.forClass(LogAuditoria.class);
        verify(repository).save(captor.capture());
        LogAuditoria log = captor.getValue();
        assertThat(log.getUsuarioId()).isEqualTo(usuarioId);
        assertThat(log.getUsuarioNome()).isEqualTo("Ana Sousa");
        assertThat(log.getPerfil()).isEqualTo(Perfil.GESTOR);
        assertThat(log.getCategoria()).isEqualTo(CategoriaAuditoria.APROVAR_RECOMENDACAO);
        assertThat(log.getAcao()).isEqualTo(CategoriaAuditoria.APROVAR_RECOMENDACAO.acaoPadrao());
        assertThat(log.getBaseLegal()).isEqualTo(CategoriaAuditoria.APROVAR_RECOMENDACAO.baseLegalPadrao());
        assertThat(log.getRecurso()).isEqualTo("recomendacao:rc-1");
        assertThat(log.isAssistidoPorIa()).isFalse();
        assertThat(log.getIp()).isEqualTo("10.0.0.9");
        assertThat(log.getData()).isNotNull();
    }

    @Test
    @DisplayName("Sem usuario autenticado, registra como Sistema (perfil/usuarioId nulos)")
    void registrarComoSistema() {
        when(contexto.atorAtual()).thenReturn(AtorAuditoria.SISTEMA);

        service.registrar(CategoriaAuditoria.INFERENCIA_IA, "minha acao", "AI Gateway · DeepSeek",
                "Anonimização (art. 12)", true);

        ArgumentCaptor<LogAuditoria> captor = ArgumentCaptor.forClass(LogAuditoria.class);
        verify(repository).save(captor.capture());
        LogAuditoria log = captor.getValue();
        assertThat(log.getUsuarioId()).isNull();
        assertThat(log.getPerfil()).isNull();
        assertThat(log.getUsuarioNome()).isEqualTo("Sistema");
        assertThat(log.getAcao()).isEqualTo("minha acao");
        assertThat(log.isAssistidoPorIa()).isTrue();
        assertThat(log.getIp()).isNull();
    }

    @Test
    @DisplayName("resumo agrega total, assistidos por IA, com base legal e ultima atividade")
    void resumoAgregaKpis() {
        Instant ultima = Instant.parse("2026-06-08T12:00:00Z");
        LogAuditoria recente = new LogAuditoria(ultima, null, "Sistema", null,
                CategoriaAuditoria.CONSULTAR, "Consultou", "L-1", "Controle sanitário", false, null);
        when(repository.count()).thenReturn(21L);
        when(repository.countByAssistidoPorIaTrue()).thenReturn(9L);
        when(repository.countByBaseLegalIsNotNull()).thenReturn(21L);
        when(repository.findTopByOrderByDataDesc()).thenReturn(Optional.of(recente));

        ResumoAuditoriaResponse resumo = service.resumo();

        assertThat(resumo.total()).isEqualTo(21L);
        assertThat(resumo.assistidosPorIa()).isEqualTo(9L);
        assertThat(resumo.comBaseLegal()).isEqualTo(21L);
        assertThat(resumo.ultimaAtividade()).isEqualTo(ultima);
    }
}
