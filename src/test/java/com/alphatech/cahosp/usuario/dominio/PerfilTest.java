package com.alphatech.cahosp.usuario.dominio;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Testes unitarios do enum {@link Perfil} (RF-ADM), com o novo perfil {@code ADMIN}. */
class PerfilTest {

    @Test
    @DisplayName("fromJson aceita o rotulo pt-BR e o nome da constante (case-insensitive)")
    void fromJsonAceitaRotuloEConstante() {
        assertThat(Perfil.fromJson("Admin")).isEqualTo(Perfil.ADMIN);
        assertThat(Perfil.fromJson("ADMIN")).isEqualTo(Perfil.ADMIN);
        assertThat(Perfil.fromJson("admin")).isEqualTo(Perfil.ADMIN);
    }

    @Test
    @DisplayName("ADMIN expoe rotulo 'Admin' e autoridade 'ROLE_ADMIN'")
    void rotuloEAutoridade() {
        assertThat(Perfil.ADMIN.rotulo()).isEqualTo("Admin");
        assertThat(Perfil.ADMIN.authority()).isEqualTo("ROLE_ADMIN");
    }

    @Test
    @DisplayName("Perfil invalido lanca IllegalArgumentException")
    void perfilInvalido() {
        assertThatThrownBy(() -> Perfil.fromJson("Root"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
