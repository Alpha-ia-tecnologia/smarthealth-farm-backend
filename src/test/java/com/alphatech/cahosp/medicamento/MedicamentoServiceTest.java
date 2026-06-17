package com.alphatech.cahosp.medicamento;

import com.alphatech.cahosp.comum.excecao.ConflitoException;
import com.alphatech.cahosp.comum.excecao.RecursoNaoEncontradoException;
import com.alphatech.cahosp.medicamento.dominio.Criticidade;
import com.alphatech.cahosp.medicamento.dominio.FamiliaTerapeutica;
import com.alphatech.cahosp.medicamento.dominio.Medicamento;
import com.alphatech.cahosp.medicamento.dto.AtualizarMedicamentoRequest;
import com.alphatech.cahosp.medicamento.dto.CriarMedicamentoRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Regra de negocio do catalogo de medicamentos (RF-DAD-06) — unitario, sem Spring/Docker. */
@ExtendWith(MockitoExtension.class)
class MedicamentoServiceTest {

    @Mock
    private MedicamentoRepository medicamentoRepository;

    @InjectMocks
    private MedicamentoService service;

    @Test
    @DisplayName("criar com codigo duplicado lanca ConflitoException")
    void criarCodigoDuplicado() {
        when(medicamentoRepository.existsByCodigoIgnoreCase("MED-001")).thenReturn(true);
        var request = new CriarMedicamentoRequest("MED-001", "Amox", "Comp",
                FamiliaTerapeutica.ANTIBIOTICOS, "cp", Criticidade.ALTA, true);

        assertThatThrownBy(() -> service.criar(request))
                .isInstanceOf(ConflitoException.class);

        verify(medicamentoRepository, never()).save(any());
    }

    @Test
    @DisplayName("criar persiste com todos os campos e ativo=true")
    void criarPersisteCampos() {
        when(medicamentoRepository.existsByCodigoIgnoreCase("MED-999")).thenReturn(false);
        when(medicamentoRepository.save(any(Medicamento.class))).thenAnswer(inv -> inv.getArgument(0));

        var request = new CriarMedicamentoRequest("MED-999", "Cefalexina 500mg", "Comprimido",
                FamiliaTerapeutica.ANTIBIOTICOS, "cp", Criticidade.MEDIA, true);
        service.criar(request);

        ArgumentCaptor<Medicamento> captor = ArgumentCaptor.forClass(Medicamento.class);
        verify(medicamentoRepository).save(captor.capture());
        Medicamento salvo = captor.getValue();
        assertThat(salvo.getCodigo()).isEqualTo("MED-999");
        assertThat(salvo.getFamilia()).isEqualTo(FamiliaTerapeutica.ANTIBIOTICOS);
        assertThat(salvo.getCriticidade()).isEqualTo(Criticidade.MEDIA);
        assertThat(salvo.isEssencial()).isTrue();
        assertThat(salvo.isAtivo()).isTrue();
    }

    @Test
    @DisplayName("atualizar id inexistente lanca RecursoNaoEncontradoException")
    void atualizarInexistente() {
        UUID id = UUID.randomUUID();
        when(medicamentoRepository.findById(id)).thenReturn(Optional.empty());
        var request = new AtualizarMedicamentoRequest("MED-X", "Nome", "Apr",
                FamiliaTerapeutica.ANALGESICOS, "cp", Criticidade.BAIXA, false);

        assertThatThrownBy(() -> service.atualizar(id, request))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    @DisplayName("atualizar para codigo de OUTRO medicamento lanca ConflitoException")
    void atualizarCodigoConflitante() {
        UUID id = UUID.randomUUID();
        Medicamento alvoMock = org.mockito.Mockito.mock(Medicamento.class);
        when(alvoMock.getCodigo()).thenReturn("MED-A");
        Medicamento outroMock = org.mockito.Mockito.mock(Medicamento.class);
        when(outroMock.getId()).thenReturn(UUID.randomUUID());

        when(medicamentoRepository.findById(id)).thenReturn(Optional.of(alvoMock));
        when(medicamentoRepository.findByCodigoIgnoreCase("MED-B")).thenReturn(Optional.of(outroMock));

        var request = new AtualizarMedicamentoRequest("MED-B", "Nome", "Apr",
                FamiliaTerapeutica.ANALGESICOS, "cp", Criticidade.BAIXA, false);

        assertThatThrownBy(() -> service.atualizar(id, request))
                .isInstanceOf(ConflitoException.class);
    }

    @Test
    @DisplayName("alterarStatus(false) desativa o medicamento")
    void desativar() {
        UUID id = UUID.randomUUID();
        Medicamento alvoMock = org.mockito.Mockito.mock(Medicamento.class);
        when(alvoMock.getFamilia()).thenReturn(FamiliaTerapeutica.ANTIBIOTICOS);
        when(alvoMock.getCriticidade()).thenReturn(Criticidade.ALTA);
        when(medicamentoRepository.findById(id)).thenReturn(Optional.of(alvoMock));

        service.alterarStatus(id, false);

        verify(alvoMock).desativar();
    }

    @Test
    @DisplayName("listar sem unidadeId usa a consulta padrao (buscarComFiltros)")
    void listarSemUnidade() {
        when(medicamentoRepository.buscarComFiltros(any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of());

        service.listar(null, null, null, null, null, null);

        verify(medicamentoRepository).buscarComFiltros(any(), any(), any(), any(), any(), any());
        verify(medicamentoRepository, never())
                .buscarPorUnidadeComFiltros(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("listar com unidadeId usa a consulta por unidade (buscarPorUnidadeComFiltros)")
    void listarComUnidade() {
        UUID unidadeId = UUID.randomUUID();
        when(medicamentoRepository.buscarPorUnidadeComFiltros(eq(unidadeId), any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of());

        service.listar(null, null, null, null, null, unidadeId);

        verify(medicamentoRepository)
                .buscarPorUnidadeComFiltros(eq(unidadeId), any(), any(), any(), any(), any(), any());
        verify(medicamentoRepository, never()).buscarComFiltros(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("buscarPorId id inexistente lanca RecursoNaoEncontradoException")
    void buscarInexistente() {
        UUID id = UUID.randomUUID();
        when(medicamentoRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscarPorId(id))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }
}
