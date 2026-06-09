package com.alphatech.cahosp.unidade;

import com.alphatech.cahosp.comum.excecao.ConflitoException;
import com.alphatech.cahosp.comum.excecao.RecursoNaoEncontradoException;
import com.alphatech.cahosp.unidade.dominio.Conectividade;
import com.alphatech.cahosp.unidade.dominio.Porte;
import com.alphatech.cahosp.unidade.dominio.Unidade;
import com.alphatech.cahosp.unidade.dto.AtualizarUnidadeRequest;
import com.alphatech.cahosp.unidade.dto.CriarUnidadeRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Regra de negocio do catalogo de unidades (RF-DAD-06) — unitario, sem Spring/Docker. */
@ExtendWith(MockitoExtension.class)
class UnidadeServiceTest {

    @Mock
    private UnidadeRepository unidadeRepository;

    @InjectMocks
    private UnidadeService service;

    @Test
    @DisplayName("criar com sigla duplicada lanca ConflitoException")
    void criarSiglaDuplicada() {
        when(unidadeRepository.existsBySiglaIgnoreCase("HTO")).thenReturn(true);
        var request = new CriarUnidadeRequest("Hospital X", "HTO", "São Luís",
                Porte.GRANDE, 100, Conectividade.ESTAVEL, "Geral", false);

        assertThatThrownBy(() -> service.criar(request))
                .isInstanceOf(ConflitoException.class);

        verify(unidadeRepository, never()).save(any());
    }

    @Test
    @DisplayName("criar persiste com todos os campos e ativo=true")
    void criarPersisteCampos() {
        when(unidadeRepository.existsBySiglaIgnoreCase("HRX")).thenReturn(false);
        when(unidadeRepository.save(any(Unidade.class))).thenAnswer(inv -> inv.getArgument(0));

        var request = new CriarUnidadeRequest("Hospital Regional X", "HRX", "Caxias",
                Porte.MEDIO, 150, Conectividade.INTERMITENTE, "Geral · interior", false);
        service.criar(request);

        ArgumentCaptor<Unidade> captor = ArgumentCaptor.forClass(Unidade.class);
        verify(unidadeRepository).save(captor.capture());
        Unidade salva = captor.getValue();
        assertThat(salva.getSigla()).isEqualTo("HRX");
        assertThat(salva.getPorte()).isEqualTo(Porte.MEDIO);
        assertThat(salva.getConectividade()).isEqualTo(Conectividade.INTERMITENTE);
        assertThat(salva.getLeitos()).isEqualTo(150);
        assertThat(salva.isHub()).isFalse();
        assertThat(salva.isAtivo()).isTrue();
    }

    @Test
    @DisplayName("atualizar id inexistente lanca RecursoNaoEncontradoException")
    void atualizarInexistente() {
        UUID id = UUID.randomUUID();
        when(unidadeRepository.findById(id)).thenReturn(Optional.empty());
        var request = new AtualizarUnidadeRequest("Nome", "SGL", "Mun",
                Porte.PEQUENO, 10, Conectividade.PRECARIA, "perfil", false);

        assertThatThrownBy(() -> service.atualizar(id, request))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    @DisplayName("atualizar para sigla de OUTRA unidade lanca ConflitoException")
    void atualizarSiglaConflitante() {
        UUID id = UUID.randomUUID();
        Unidade alvo = org.mockito.Mockito.mock(Unidade.class);
        when(alvo.getSigla()).thenReturn("HRA");
        Unidade outra = org.mockito.Mockito.mock(Unidade.class);
        when(outra.getId()).thenReturn(UUID.randomUUID());

        when(unidadeRepository.findById(id)).thenReturn(Optional.of(alvo));
        when(unidadeRepository.findBySiglaIgnoreCase("HRB")).thenReturn(Optional.of(outra));

        var request = new AtualizarUnidadeRequest("Hospital A", "HRB", "X",
                Porte.PEQUENO, 50, Conectividade.PRECARIA, "perfil", false);

        assertThatThrownBy(() -> service.atualizar(id, request))
                .isInstanceOf(ConflitoException.class);
    }

    @Test
    @DisplayName("alterarStatus(false) desativa a unidade")
    void desativar() {
        UUID id = UUID.randomUUID();
        Unidade unidadeMock = org.mockito.Mockito.mock(Unidade.class);
        when(unidadeMock.getPorte()).thenReturn(Porte.PEQUENO);
        when(unidadeMock.getConectividade()).thenReturn(Conectividade.PRECARIA);
        when(unidadeRepository.findById(id)).thenReturn(Optional.of(unidadeMock));

        service.alterarStatus(id, false);

        verify(unidadeMock).desativar();
    }

    @Test
    @DisplayName("buscarPorId id inexistente lanca RecursoNaoEncontradoException")
    void buscarInexistente() {
        UUID id = UUID.randomUUID();
        when(unidadeRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscarPorId(id))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }
}
