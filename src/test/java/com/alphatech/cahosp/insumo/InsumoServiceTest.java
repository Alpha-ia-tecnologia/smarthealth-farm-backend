package com.alphatech.cahosp.insumo;

import com.alphatech.cahosp.comum.excecao.ConflitoException;
import com.alphatech.cahosp.comum.excecao.RecursoNaoEncontradoException;
import com.alphatech.cahosp.insumo.dominio.Criticidade;
import com.alphatech.cahosp.insumo.dominio.CategoriaInsumo;
import com.alphatech.cahosp.insumo.dominio.Insumo;
import com.alphatech.cahosp.insumo.dto.AtualizarInsumoRequest;
import com.alphatech.cahosp.insumo.dto.CriarInsumoRequest;
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

/** Regra de negocio do catalogo de insumos (RF-DAD-06) — unitario, sem Spring/Docker. */
@ExtendWith(MockitoExtension.class)
class InsumoServiceTest {

    @Mock
    private InsumoRepository insumoRepository;

    @InjectMocks
    private InsumoService service;

    @Test
    @DisplayName("criar com codigo duplicado lanca ConflitoException")
    void criarCodigoDuplicado() {
        when(insumoRepository.existsByCodigoIgnoreCase("INS-001")).thenReturn(true);
        var request = new CriarInsumoRequest("INS-001", "Amox", "Comp",
                CategoriaInsumo.ANTIBIOTICOS, "cp", Criticidade.ALTA, true);

        assertThatThrownBy(() -> service.criar(request))
                .isInstanceOf(ConflitoException.class);

        verify(insumoRepository, never()).save(any());
    }

    @Test
    @DisplayName("criar persiste com todos os campos e ativo=true")
    void criarPersisteCampos() {
        when(insumoRepository.existsByCodigoIgnoreCase("INS-999")).thenReturn(false);
        when(insumoRepository.save(any(Insumo.class))).thenAnswer(inv -> inv.getArgument(0));

        var request = new CriarInsumoRequest("INS-999", "Cefalexina 500mg", "Comprimido",
                CategoriaInsumo.ANTIBIOTICOS, "cp", Criticidade.MEDIA, true);
        service.criar(request);

        ArgumentCaptor<Insumo> captor = ArgumentCaptor.forClass(Insumo.class);
        verify(insumoRepository).save(captor.capture());
        Insumo salvo = captor.getValue();
        assertThat(salvo.getCodigo()).isEqualTo("INS-999");
        assertThat(salvo.getCategoria()).isEqualTo(CategoriaInsumo.ANTIBIOTICOS);
        assertThat(salvo.getCriticidade()).isEqualTo(Criticidade.MEDIA);
        assertThat(salvo.isEssencial()).isTrue();
        assertThat(salvo.isAtivo()).isTrue();
    }

    @Test
    @DisplayName("atualizar id inexistente lanca RecursoNaoEncontradoException")
    void atualizarInexistente() {
        UUID id = UUID.randomUUID();
        when(insumoRepository.findById(id)).thenReturn(Optional.empty());
        var request = new AtualizarInsumoRequest("INS-X", "Nome", "Apr",
                CategoriaInsumo.ANALGESICOS, "cp", Criticidade.BAIXA, false);

        assertThatThrownBy(() -> service.atualizar(id, request))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    @DisplayName("atualizar para codigo de OUTRO insumo lanca ConflitoException")
    void atualizarCodigoConflitante() {
        UUID id = UUID.randomUUID();
        Insumo alvoMock = org.mockito.Mockito.mock(Insumo.class);
        when(alvoMock.getCodigo()).thenReturn("INS-A");
        Insumo outroMock = org.mockito.Mockito.mock(Insumo.class);
        when(outroMock.getId()).thenReturn(UUID.randomUUID());

        when(insumoRepository.findById(id)).thenReturn(Optional.of(alvoMock));
        when(insumoRepository.findByCodigoIgnoreCase("INS-B")).thenReturn(Optional.of(outroMock));

        var request = new AtualizarInsumoRequest("INS-B", "Nome", "Apr",
                CategoriaInsumo.ANALGESICOS, "cp", Criticidade.BAIXA, false);

        assertThatThrownBy(() -> service.atualizar(id, request))
                .isInstanceOf(ConflitoException.class);
    }

    @Test
    @DisplayName("alterarStatus(false) desativa o insumo")
    void desativar() {
        UUID id = UUID.randomUUID();
        Insumo alvoMock = org.mockito.Mockito.mock(Insumo.class);
        when(alvoMock.getCategoria()).thenReturn(CategoriaInsumo.ANTIBIOTICOS);
        when(alvoMock.getCriticidade()).thenReturn(Criticidade.ALTA);
        when(insumoRepository.findById(id)).thenReturn(Optional.of(alvoMock));

        service.alterarStatus(id, false);

        verify(alvoMock).desativar();
    }

    @Test
    @DisplayName("listar sem unidadeId usa a consulta padrao (buscarComFiltros)")
    void listarSemUnidade() {
        when(insumoRepository.buscarComFiltros(any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of());

        service.listar(null, null, null, null, null, null);

        verify(insumoRepository).buscarComFiltros(any(), any(), any(), any(), any(), any());
        verify(insumoRepository, never())
                .buscarPorUnidadeComFiltros(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("listar com unidadeId usa a consulta por unidade (buscarPorUnidadeComFiltros)")
    void listarComUnidade() {
        UUID unidadeId = UUID.randomUUID();
        when(insumoRepository.buscarPorUnidadeComFiltros(eq(unidadeId), any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of());

        service.listar(null, null, null, null, null, unidadeId);

        verify(insumoRepository)
                .buscarPorUnidadeComFiltros(eq(unidadeId), any(), any(), any(), any(), any(), any());
        verify(insumoRepository, never()).buscarComFiltros(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("buscarPorId id inexistente lanca RecursoNaoEncontradoException")
    void buscarInexistente() {
        UUID id = UUID.randomUUID();
        when(insumoRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscarPorId(id))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }
}
