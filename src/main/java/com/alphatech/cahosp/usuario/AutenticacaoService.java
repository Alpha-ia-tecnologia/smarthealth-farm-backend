package com.alphatech.cahosp.usuario;

import com.alphatech.cahosp.seguranca.JwtService;
import com.alphatech.cahosp.usuario.dominio.Usuario;
import com.alphatech.cahosp.usuario.dto.LoginRequest;
import com.alphatech.cahosp.usuario.dto.LoginResponse;
import com.alphatech.cahosp.usuario.dto.UsuarioResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Regra de autenticacao: valida credenciais, registra o ultimo acesso e emite o JWT.
 * RF-SEG. Falhas de credencial/conta sobem como {@code AuthenticationException} e sao
 * convertidas para 401 pelo handler global.
 */
@Service
public class AutenticacaoService {

    private final AuthenticationManager authenticationManager;
    private final UsuarioRepository usuarioRepository;
    private final JwtService jwtService;

    public AutenticacaoService(AuthenticationManager authenticationManager,
                               UsuarioRepository usuarioRepository,
                               JwtService jwtService) {
        this.authenticationManager = authenticationManager;
        this.usuarioRepository = usuarioRepository;
        this.jwtService = jwtService;
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        // Valida e-mail/senha e estado da conta (lanca AuthenticationException em falha).
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));

        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(request.email())
                .orElseThrow();
        usuario.registrarAcesso(Instant.now());

        String token = jwtService.gerarToken(usuario);
        return new LoginResponse(UsuarioResponse.de(usuario), token);
    }
}
