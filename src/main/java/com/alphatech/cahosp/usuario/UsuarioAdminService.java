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
import com.alphatech.cahosp.usuario.dto.UsuarioResponse;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Regra de negocio da administracao de usuarios (RF-ADM-01). Exclusivo do perfil TI.
 *
 * <p>Nao ha exclusao fisica (LGPD/auditoria): o desligamento e feito por desativacao.
 * Senhas sempre passam por {@link PasswordEncoder} (BCrypt) — nunca texto puro.
 */
@Service
public class UsuarioAdminService {

    private final UsuarioRepository usuarioRepository;
    private final UnidadeRepository unidadeRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioAdminService(UsuarioRepository usuarioRepository,
                               UnidadeRepository unidadeRepository,
                               PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.unidadeRepository = unidadeRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /** Lista usuarios com filtros opcionais (perfil, ativo, busca em nome/e-mail). RF-ADM-01. */
    @Transactional(readOnly = true)
    public List<UsuarioResponse> listar(Perfil perfil, Boolean ativo, String busca) {
        String termo = (busca == null || busca.isBlank()) ? null : busca.trim();
        return usuarioRepository.buscarComFiltros(perfil, ativo, termo, Sort.by("nome").ascending())
                .stream()
                .map(UsuarioResponse::de)
                .toList();
    }

    /** Busca um usuario pelo id; 404 se nao existir. RF-ADM-01. */
    @Transactional(readOnly = true)
    public UsuarioResponse buscarPorId(UUID id) {
        return UsuarioResponse.de(obter(id));
    }

    /** Cria um usuario; e-mail unico (ignore-case) sob pena de conflito. RF-ADM-01. */
    @Transactional
    public UsuarioResponse criar(CriarUsuarioRequest request) {
        if (usuarioRepository.existsByEmailIgnoreCase(request.email())) {
            throw new ConflitoException("Ja existe um usuario com o e-mail '" + request.email() + "'.");
        }
        Usuario usuario = new Usuario(
                request.nome(),
                request.email(),
                passwordEncoder.encode(request.senha()),
                request.perfil());
        usuario.setUnidade(resolverUnidade(request.unidadeId()));
        return UsuarioResponse.de(usuarioRepository.save(usuario));
    }

    /** Atualiza nome, e-mail e perfil; e-mail novo nao pode pertencer a OUTRO usuario. RF-ADM-01. */
    @Transactional
    public UsuarioResponse atualizar(UUID id, AtualizarUsuarioRequest request) {
        Usuario usuario = obter(id);
        if (!usuario.getEmail().equalsIgnoreCase(request.email())) {
            usuarioRepository.findByEmailIgnoreCase(request.email())
                    .filter(outro -> !outro.getId().equals(id))
                    .ifPresent(outro -> {
                        throw new ConflitoException(
                                "Ja existe um usuario com o e-mail '" + request.email() + "'.");
                    });
        }
        usuario.setNome(request.nome());
        usuario.setEmail(request.email());
        usuario.setPerfil(request.perfil());
        usuario.setUnidade(resolverUnidade(request.unidadeId()));
        return UsuarioResponse.de(usuario);
    }

    /**
     * Ativa/desativa um usuario. Regra: o TI autenticado nao pode desativar a propria conta
     * (evita lockout administrativo). RF-ADM-01.
     */
    @Transactional
    public UsuarioResponse alterarStatus(UUID id, boolean ativo, UUID idAutenticado) {
        Usuario usuario = obter(id);
        if (!ativo && usuario.getId().equals(idAutenticado)) {
            throw new RegraNegocioException("Voce nao pode desativar a propria conta.");
        }
        if (ativo) {
            usuario.ativar();
        } else {
            usuario.desativar();
        }
        return UsuarioResponse.de(usuario);
    }

    /** Redefine a senha (hash BCrypt); 404 se o usuario nao existir. RF-ADM-01. */
    @Transactional
    public void redefinirSenha(UUID id, String novaSenha) {
        Usuario usuario = obter(id);
        usuario.setSenhaHash(passwordEncoder.encode(novaSenha));
    }

    private Usuario obter(UUID id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Usuario nao encontrado: " + id + "."));
    }

    /** Resolve a unidade de lotacao opcional; null = sem unidade. 404 se o id nao existir. */
    private Unidade resolverUnidade(UUID unidadeId) {
        if (unidadeId == null) {
            return null;
        }
        return unidadeRepository.findById(unidadeId)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Unidade nao encontrada: " + unidadeId + "."));
    }
}
