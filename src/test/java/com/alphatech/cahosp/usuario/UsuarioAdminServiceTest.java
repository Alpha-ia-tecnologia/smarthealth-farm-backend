package com.alphatech.cahosp.usuario;

import com.alphatech.cahosp.comum.excecao.ConflitoException;
import com.alphatech.cahosp.comum.excecao.RecursoNaoEncontradoException;
import com.alphatech.cahosp.comum.excecao.RegraNegocioException;
import com.alphatech.cahosp.unidade.UnidadeRepository;
import com.alphatech.cahosp.unidade.dominio.Unidade;
import com.alphatech.cahosp.usuario.dominio.Perfil;
import com.alphatech.cahosp.usuario.dominio.Usuario;
import com.alphatech.cahosp.usuario.dto.AtualizarUsuarioRequest;
import com.alphatech.cahosp.usuario.dto.CriarUsuarioRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Regra de negocio da administracao de usuarios (RF-ADM-01) — unitario, sem Spring/Docker.
 */
@ExtendWith(MockitoExtension.class)
class UsuarioAdminServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private UnidadeRepository unidadeRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UsuarioAdminService service;

    @Test
    @DisplayName("criar com e-mail duplicado lanca ConflitoException")
    void criarEmailDuplicado() {
        when(usuarioRepository.existsByEmailIgnoreCase("dup@cahosp.local")).thenReturn(true);
        var request = new CriarUsuarioRequest("Maria", "dup@cahosp.local", Perfil.OPERADOR, "senha1234", null);

        assertThatThrownBy(() -> service.criar(request))
                .isInstanceOf(ConflitoException.class);

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("criar com sucesso grava a senha como hash (encoder usado), nunca texto puro")
    void criarUsaEncoder() {
        when(usuarioRepository.existsByEmailIgnoreCase(anyString())).thenReturn(false);
        when(passwordEncoder.encode("senha1234")).thenReturn("HASH-BCRYPT");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        var request = new CriarUsuarioRequest("Maria", "maria@cahosp.local", Perfil.GESTOR, "senha1234", null);
        service.criar(request);

        verify(passwordEncoder).encode("senha1234");
        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        assertThat(captor.getValue().getSenhaHash()).isEqualTo("HASH-BCRYPT");
        assertThat(captor.getValue().getSenhaHash()).isNotEqualTo("senha1234");
        assertThat(captor.getValue().getUnidade()).isNull();
    }

    @Test
    @DisplayName("criar com unidadeId resolve a unidade e a vincula ao usuario")
    void criarComUnidade() {
        UUID unidadeId = UUID.randomUUID();
        Unidade unidade = org.mockito.Mockito.mock(Unidade.class);
        when(usuarioRepository.existsByEmailIgnoreCase(anyString())).thenReturn(false);
        when(unidadeRepository.findById(unidadeId)).thenReturn(Optional.of(unidade));
        when(passwordEncoder.encode(anyString())).thenReturn("HASH");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        var request = new CriarUsuarioRequest("Ana", "ana@cahosp.local", Perfil.OPERADOR, "senha1234", unidadeId);
        service.criar(request);

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        assertThat(captor.getValue().getUnidade()).isSameAs(unidade);
    }

    @Test
    @DisplayName("criar com unidadeId inexistente lanca RecursoNaoEncontradoException")
    void criarComUnidadeInexistente() {
        UUID unidadeId = UUID.randomUUID();
        when(usuarioRepository.existsByEmailIgnoreCase(anyString())).thenReturn(false);
        when(unidadeRepository.findById(unidadeId)).thenReturn(Optional.empty());

        var request = new CriarUsuarioRequest("Ana", "ana@cahosp.local", Perfil.OPERADOR, "senha1234", unidadeId);

        assertThatThrownBy(() -> service.criar(request))
                .isInstanceOf(RecursoNaoEncontradoException.class);
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("atualizar id inexistente lanca RecursoNaoEncontradoException")
    void atualizarInexistente() {
        UUID id = UUID.randomUUID();
        when(usuarioRepository.findById(id)).thenReturn(Optional.empty());
        var request = new AtualizarUsuarioRequest("Novo", "novo@cahosp.local", Perfil.TI, null);

        assertThatThrownBy(() -> service.atualizar(id, request))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    @DisplayName("desativar o PROPRIO id lanca RegraNegocioException (422)")
    void naoDesativaPropriaConta() {
        UUID id = UUID.randomUUID();
        Usuario eu = mockUsuarioComId(id);
        when(usuarioRepository.findById(id)).thenReturn(Optional.of(eu));

        assertThatThrownBy(() -> service.alterarStatus(id, false, id))
                .isInstanceOf(RegraNegocioException.class);

        verify(eu, never()).desativar();
    }

    @Test
    @DisplayName("redefinirSenha grava o novo hash (encoder usado)")
    void redefinirSenhaUsaEncoder() {
        UUID id = UUID.randomUUID();
        Usuario alvo = org.mockito.Mockito.mock(Usuario.class);
        when(usuarioRepository.findById(id)).thenReturn(Optional.of(alvo));
        when(passwordEncoder.encode("novaSenha8")).thenReturn("NOVO-HASH");

        service.redefinirSenha(id, "novaSenha8");

        verify(passwordEncoder).encode("novaSenha8");
        verify(alvo).setSenhaHash("NOVO-HASH");
    }

    private Usuario mockUsuarioComId(UUID id) {
        Usuario usuario = org.mockito.Mockito.mock(Usuario.class);
        when(usuario.getId()).thenReturn(id);
        return usuario;
    }
}
