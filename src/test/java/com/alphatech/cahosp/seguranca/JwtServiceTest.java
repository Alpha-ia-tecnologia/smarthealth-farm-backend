package com.alphatech.cahosp.seguranca;

import com.alphatech.cahosp.usuario.dominio.Perfil;
import com.alphatech.cahosp.usuario.dominio.Usuario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testes unitarios do {@link JwtService} (sem Spring, sem Docker).
 */
class JwtServiceTest {

    private static final String SEGREDO = "segredo-de-teste-com-no-minimo-32-bytes-aqui-ok";
    private static final String OUTRO_SEGREDO = "outro-segredo-completamente-diferente-com-32b";

    private Usuario usuario() {
        return new Usuario("Ana Sousa", "ana.sousa@emserh.ma.gov.br", "hash-bcrypt", Perfil.GESTOR);
    }

    @Test
    @DisplayName("Token gerado carrega o e-mail (subject) e o perfil, e e valido")
    void gerarEValidar() {
        JwtService jwt = new JwtService(SEGREDO, 3_600_000L);
        String token = jwt.gerarToken(usuario());

        assertThat(jwt.tokenValido(token)).isTrue();
        assertThat(jwt.extrairEmail(token)).isEqualTo("ana.sousa@emserh.ma.gov.br");
        assertThat(jwt.extrairPerfil(token)).isEqualTo(Perfil.GESTOR);
    }

    @Test
    @DisplayName("Token assinado com outro segredo nao e aceito")
    void rejeitaAssinaturaDiferente() {
        String token = new JwtService(SEGREDO, 3_600_000L).gerarToken(usuario());
        JwtService outro = new JwtService(OUTRO_SEGREDO, 3_600_000L);

        assertThat(outro.tokenValido(token)).isFalse();
    }

    @Test
    @DisplayName("Token expirado e rejeitado")
    void rejeitaExpirado() {
        JwtService jwt = new JwtService(SEGREDO, -1_000L); // expira no passado
        String token = jwt.gerarToken(usuario());

        assertThat(jwt.tokenValido(token)).isFalse();
    }

    @Test
    @DisplayName("Texto que nao e um JWT e rejeitado")
    void rejeitaLixo() {
        JwtService jwt = new JwtService(SEGREDO, 3_600_000L);

        assertThat(jwt.tokenValido("nao-e-um-token")).isFalse();
    }

    @Test
    @DisplayName("Segredo curto (< 32 bytes) e recusado na construcao")
    void recusaSegredoCurto() {
        assertThatThrownBy(() -> new JwtService("curto", 3_600_000L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32 bytes");
    }
}
