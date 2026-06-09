package com.alphatech.cahosp.medicamento.dto;

import com.alphatech.cahosp.medicamento.dominio.Criticidade;
import com.alphatech.cahosp.medicamento.dominio.FamiliaTerapeutica;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Dados para criar um medicamento (RF-DAD-06). O codigo e o codigo de negocio legivel
 * (ex.: {@code MED-001}), unico e usado para integracao com sistemas externos.
 */
public record CriarMedicamentoRequest(
        @NotBlank(message = "O codigo e obrigatorio.")
        @Size(max = 30, message = "O codigo deve ter no maximo 30 caracteres.")
        String codigo,

        @NotBlank(message = "O nome e obrigatorio.")
        String nome,

        @NotBlank(message = "A apresentacao e obrigatoria.")
        String apresentacao,

        @NotNull(message = "A familia terapeutica e obrigatoria.")
        FamiliaTerapeutica familia,

        @NotBlank(message = "A unidade de medida e obrigatoria.")
        @Size(max = 20, message = "A unidade de medida deve ter no maximo 20 caracteres.")
        String unidadeMedida,

        @NotNull(message = "A criticidade e obrigatoria.")
        Criticidade criticidade,

        boolean essencial
) {
}
