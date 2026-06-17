package com.alphatech.cahosp.usuario;

import com.alphatech.cahosp.usuario.dominio.Perfil;
import com.alphatech.cahosp.usuario.dominio.Usuario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Cria o administrador inicial (perfil {@code ADMIN}, superusuario) no startup, SE ainda nao
 * existir — idempotente em redeploys. Se o usuario do e-mail configurado ja existe, nao faz nada.
 * Credenciais vem de env ({@code ADMIN_NOME}/{@code ADMIN_EMAIL}/{@code ADMIN_SENHA}). Sem senha
 * definida, nao cria nada (evita admin com senha fraca). RF-ADM / RF-SEG.
 */
@Component
public class AdminSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminSeeder.class);

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final String nome;
    private final String email;
    private final String senha;

    public AdminSeeder(UsuarioRepository usuarioRepository,
                       PasswordEncoder passwordEncoder,
                       @Value("${app.admin.nome}") String nome,
                       @Value("${app.admin.email}") String email,
                       @Value("${app.admin.senha}") String senha) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.nome = nome;
        this.email = email;
        this.senha = senha;
    }

    @Override
    public void run(String... args) {
        if (senha == null || senha.isBlank()) {
            log.warn("ADMIN_SENHA nao definida — administrador inicial NAO foi criado.");
            return;
        }
        if (usuarioRepository.findByEmailIgnoreCase(email).isPresent()) {
            log.info("Administrador inicial ja existe ({}). Nada a fazer.", email);
            return;
        }
        Usuario admin = new Usuario(nome, email, passwordEncoder.encode(senha), Perfil.ADMIN);
        usuarioRepository.save(admin);
        log.info("Administrador inicial criado: {} (perfil ADMIN).", email);
    }
}
