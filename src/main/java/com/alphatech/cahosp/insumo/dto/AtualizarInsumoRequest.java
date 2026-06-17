package com.alphatech.cahosp.insumo.dto;

import com.alphatech.cahosp.insumo.dominio.Criticidade;
import com.alphatech.cahosp.insumo.dominio.CategoriaInsumo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Atualizacao de insumo (RF-DAD-06). Status tem endpoint proprio. */
public record AtualizarInsumoRequest(
        @NotBlank(message = "O codigo e obrigatorio.")
        @Size(max = 30, message = "O codigo deve ter no maximo 30 caracteres.")
        String codigo,

        @NotBlank(message = "O nome e obrigatorio.")
        String nome,

        @NotBlank(message = "A apresentacao e obrigatoria.")
        String apresentacao,

        @NotNull(message = "A categoria e obrigatoria.")
        CategoriaInsumo categoria,

        @NotBlank(message = "A unidade de medida e obrigatoria.")
        @Size(max = 20, message = "A unidade de medida deve ter no maximo 20 caracteres.")
        String unidadeMedida,

        @NotNull(message = "A criticidade e obrigatoria.")
        Criticidade criticidade,

        boolean essencial
) {
}
