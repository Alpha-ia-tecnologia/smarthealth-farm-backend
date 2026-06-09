package com.alphatech.cahosp.unidade.dto;

import com.alphatech.cahosp.unidade.dominio.Conectividade;
import com.alphatech.cahosp.unidade.dominio.Porte;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * Dados para criar uma unidade (RF-DAD-06). Sigla e usada como codigo de negocio (unica).
 * Os enums aceitam tanto o nome da constante quanto o rotulo pt-BR.
 */
public record CriarUnidadeRequest(
        @NotBlank(message = "O nome e obrigatorio.")
        String nome,

        @NotBlank(message = "A sigla e obrigatoria.")
        @Size(max = 20, message = "A sigla deve ter no maximo 20 caracteres.")
        String sigla,

        @NotBlank(message = "O municipio e obrigatorio.")
        String municipio,

        @NotNull(message = "O porte e obrigatorio.")
        Porte porte,

        @PositiveOrZero(message = "O numero de leitos nao pode ser negativo.")
        int leitos,

        @NotNull(message = "A conectividade e obrigatoria.")
        Conectividade conectividade,

        @NotBlank(message = "O perfil demografico e obrigatorio.")
        String perfilDemografico,

        boolean hub
) {
}
