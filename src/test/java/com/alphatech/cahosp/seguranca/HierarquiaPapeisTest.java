package com.alphatech.cahosp.seguranca;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes unitarios da hierarquia de papeis (RF-ADM / RF-SEG): o {@code ADMIN} herda Gestor e TI
 * (e, por eles, Operador); Gestor e TI sao independentes entre si. Garante que um Admin satisfaz
 * qualquer {@code hasRole('GESTOR')}/{@code 'TI'} sem checagem caso a caso.
 */
class HierarquiaPapeisTest {

    private final RoleHierarchy hierarquia = SecurityConfig.construirHierarquiaPapeis();

    private List<String> alcancaveis(String authority) {
        return hierarquia
                .getReachableGrantedAuthorities(List.of(new SimpleGrantedAuthority(authority)))
                .stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
    }

    @Test
    @DisplayName("ADMIN alcanca GESTOR, TI e OPERADOR (superusuario)")
    void adminHerdaTudo() {
        assertThat(alcancaveis("ROLE_ADMIN"))
                .contains("ROLE_ADMIN", "ROLE_GESTOR", "ROLE_TI", "ROLE_OPERADOR");
    }

    @Test
    @DisplayName("GESTOR alcanca OPERADOR, mas nao TI nem ADMIN")
    void gestorNaoEhAdmin() {
        assertThat(alcancaveis("ROLE_GESTOR")).contains("ROLE_GESTOR", "ROLE_OPERADOR");
        assertThat(alcancaveis("ROLE_GESTOR")).doesNotContain("ROLE_TI", "ROLE_ADMIN");
    }

    @Test
    @DisplayName("TI alcanca OPERADOR, mas nao GESTOR nem ADMIN")
    void tiNaoEhGestor() {
        assertThat(alcancaveis("ROLE_TI")).contains("ROLE_TI", "ROLE_OPERADOR");
        assertThat(alcancaveis("ROLE_TI")).doesNotContain("ROLE_GESTOR", "ROLE_ADMIN");
    }
}
