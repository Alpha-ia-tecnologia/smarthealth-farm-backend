package com.alphatech.cahosp.recomendacao;

import com.alphatech.cahosp.comum.excecao.RegraNegocioException;
import com.alphatech.cahosp.insumo.InsumoRepository;
import com.alphatech.cahosp.insumo.dominio.Insumo;
import com.alphatech.cahosp.recomendacao.dominio.OrigemMotor;
import com.alphatech.cahosp.recomendacao.dominio.Prioridade;
import com.alphatech.cahosp.recomendacao.dominio.Recomendacao;
import com.alphatech.cahosp.recomendacao.dominio.StatusRecomendacao;
import com.alphatech.cahosp.recomendacao.dominio.TipoRecomendacao;
import com.alphatech.cahosp.recomendacao.dto.CriarRecomendacaoRequest;
import com.alphatech.cahosp.recomendacao.dto.EditarRecomendacaoRequest;
import com.alphatech.cahosp.seguranca.auditoria.RegistradorAuditoria;
import com.alphatech.cahosp.seguranca.auditoria.dominio.CategoriaAuditoria;
import com.alphatech.cahosp.unidade.UnidadeRepository;
import com.alphatech.cahosp.unidade.dominio.Unidade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testes unitarios das acoes manuais de recomendacao (RF-REC-05): criar (transferencia manual),
 * editar (pendente) e recusar — com economia calculada pela {@link CalculadoraRecomendacao} real e
 * registro na auditoria. As regras de transicao de status sao garantidas pela entidade.
 */
@ExtendWith(MockitoExtension.class)
class RecomendacaoServiceTest {

    @Mock private RecomendacaoRepository recomendacaoRepository;
    @Mock private GeradorRecomendacao geradorRecomendacao;
    @Mock private InsumoRepository insumoRepository;
    @Mock private UnidadeRepository unidadeRepository;
    @Mock private RegistradorAuditoria auditoria;

    private RecomendacaoService service;

    @BeforeEach
    void init() {
        service = new RecomendacaoService(recomendacaoRepository, geradorRecomendacao,
                new CalculadoraRecomendacao(), insumoRepository, unidadeRepository, auditoria);
    }

    private Recomendacao redistribuicaoPendente() {
        return new Recomendacao(TipoRecomendacao.REDISTRIBUICAO, mock(Insumo.class),
                mock(Unidade.class), mock(Unidade.class), 20, "j", OrigemMotor.MANUAL,
                Prioridade.IMPORTANTE, new BigDecimal("100.00"));
    }

    @Test
    @DisplayName("criar gera uma redistribuição MANUAL pendente, economia = qtd x 12, e audita")
    void criarTransferenciaManual() {
        UUID medId = UUID.randomUUID();
        UUID origemId = UUID.randomUUID();
        UUID destinoId = UUID.randomUUID();
        when(insumoRepository.findById(medId)).thenReturn(Optional.of(mock(Insumo.class)));
        when(unidadeRepository.findById(origemId)).thenReturn(Optional.of(mock(Unidade.class)));
        when(unidadeRepository.findById(destinoId)).thenReturn(Optional.of(mock(Unidade.class)));
        when(recomendacaoRepository.save(any(Recomendacao.class))).thenAnswer(inv -> inv.getArgument(0));

        service.criar(new CriarRecomendacaoRequest(medId, origemId, destinoId, 10, null, null));

        ArgumentCaptor<Recomendacao> captor = ArgumentCaptor.forClass(Recomendacao.class);
        verify(recomendacaoRepository).save(captor.capture());
        Recomendacao salvo = captor.getValue();
        assertThat(salvo.getTipo()).isEqualTo(TipoRecomendacao.REDISTRIBUICAO);
        assertThat(salvo.getOrigemMotor()).isEqualTo(OrigemMotor.MANUAL);
        assertThat(salvo.getStatus()).isEqualTo(StatusRecomendacao.PENDENTE);
        assertThat(salvo.getQuantidade()).isEqualTo(10);
        assertThat(salvo.getEconomiaEstimada()).isEqualByComparingTo("120.00");
        verify(auditoria).registrar(eq(CategoriaAuditoria.CRIAR_RECOMENDACAO), anyString());
    }

    @Test
    @DisplayName("criar com origem igual ao destino => RegraNegocioException, sem salvar")
    void criarComOrigemIgualDestino() {
        UUID medId = UUID.randomUUID();
        UUID mesma = UUID.randomUUID();

        assertThatThrownBy(() -> service.criar(new CriarRecomendacaoRequest(medId, mesma, mesma, 10, null, null)))
                .isInstanceOf(RegraNegocioException.class);
        verify(recomendacaoRepository, never()).save(any());
    }

    @Test
    @DisplayName("editar uma pendente recalcula a economia (qtd x 12), mantém Pendente e audita")
    void editarPendente() {
        Recomendacao rec = redistribuicaoPendente();
        UUID id = UUID.randomUUID();
        UUID medId = UUID.randomUUID();
        UUID origemId = UUID.randomUUID();
        UUID destinoId = UUID.randomUUID();
        when(recomendacaoRepository.findComRelacionamentos(id)).thenReturn(Optional.of(rec));
        when(insumoRepository.findById(medId)).thenReturn(Optional.of(mock(Insumo.class)));
        when(unidadeRepository.findById(origemId)).thenReturn(Optional.of(mock(Unidade.class)));
        when(unidadeRepository.findById(destinoId)).thenReturn(Optional.of(mock(Unidade.class)));
        when(recomendacaoRepository.save(any(Recomendacao.class))).thenAnswer(inv -> inv.getArgument(0));

        service.editar(id, new EditarRecomendacaoRequest(medId, origemId, destinoId, 5));

        assertThat(rec.getQuantidade()).isEqualTo(5);
        assertThat(rec.getEconomiaEstimada()).isEqualByComparingTo("60.00");
        assertThat(rec.getStatus()).isEqualTo(StatusRecomendacao.PENDENTE);
        verify(auditoria).registrar(eq(CategoriaAuditoria.EDITAR_RECOMENDACAO), eq("recomendacao:" + id));
    }

    @Test
    @DisplayName("recusar uma pendente muda para Recusada e audita")
    void recusarPendente() {
        Recomendacao rec = redistribuicaoPendente();
        UUID id = UUID.randomUUID();
        when(recomendacaoRepository.findComRelacionamentos(id)).thenReturn(Optional.of(rec));
        when(recomendacaoRepository.save(any(Recomendacao.class))).thenAnswer(inv -> inv.getArgument(0));

        service.recusar(id);

        assertThat(rec.getStatus()).isEqualTo(StatusRecomendacao.RECUSADA);
        verify(auditoria).registrar(eq(CategoriaAuditoria.RECUSAR_RECOMENDACAO), eq("recomendacao:" + id));
    }

    @Test
    @DisplayName("recusar uma já aprovada => RegraNegocioException (só pendente pode ser recusada)")
    void recusarNaoPendente() {
        Recomendacao rec = redistribuicaoPendente();
        rec.aprovar();
        UUID id = UUID.randomUUID();
        when(recomendacaoRepository.findComRelacionamentos(id)).thenReturn(Optional.of(rec));

        assertThatThrownBy(() -> service.recusar(id)).isInstanceOf(RegraNegocioException.class);
        verify(auditoria, never()).registrar(any(), anyString());
    }
}
