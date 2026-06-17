package com.alphatech.cahosp.usuario.dominio;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

/**
 * Perfil de acesso (RBAC). RF-ADM / RF-SEG.
 *
 * <ul>
 *   <li>{@code OPERADOR} — operacao do dia a dia (leitura e lancamentos operacionais).</li>
 *   <li>{@code GESTOR} — decisoes de negocio (aprovar recomendacao, recalibrar previsao, limiares).</li>
 *   <li>{@code TI} — administracao do sistema (usuarios, parametros, integracoes).</li>
 *   <li>{@code ADMIN} — superusuario: acumula todos os poderes (Gestor + TI). Via hierarquia de
 *       papeis no Spring Security ({@code ROLE_ADMIN > ROLE_GESTOR}, {@code > ROLE_TI}), qualquer
 *       {@code @PreAuthorize} de Gestor/TI ja aceita um Admin, sem checagem caso a caso.</li>
 * </ul>
 *
 * <p>Os rotulos do front sao "Operador"/"Gestor"/"TI"/"Admin"; aqui o nome da constante e o valor
 * persistido em maiusculo. A autoridade do Spring Security e {@code ROLE_<constante>}.
 */
public enum Perfil {
    OPERADOR("Operador"),
    GESTOR("Gestor"),
    TI("TI"),
    ADMIN("Admin");

    private final String rotulo;

    Perfil(String rotulo) {
        this.rotulo = rotulo;
    }

    /**
     * Rotulo exibido no frontend (pt-BR). Tambem e o valor serializado em JSON
     * (respostas falam "Operador"/"Gestor"/"TI").
     */
    @JsonValue
    public String rotulo() {
        return rotulo;
    }

    /** Autoridade usada pelo Spring Security (ex.: {@code ROLE_GESTOR}). */
    public String authority() {
        return "ROLE_" + name();
    }

    /**
     * Desserializa a partir do JSON, aceitando TANTO o nome da constante ("OPERADOR")
     * QUANTO o rotulo pt-BR ("Operador"), ignorando maiusculas/minusculas. RF-ADM.
     */
    @JsonCreator
    public static Perfil fromJson(String valor) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("Perfil e obrigatorio.");
        }
        String alvo = valor.trim();
        return Arrays.stream(values())
                .filter(p -> p.name().equalsIgnoreCase(alvo) || p.rotulo.equalsIgnoreCase(alvo))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Perfil invalido: '" + valor + "'. Use Operador, Gestor, TI ou Admin."));
    }
}
