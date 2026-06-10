package com.alphatech.cahosp.ia;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes unitarios da anonimizacao (RF-SEG-04): mascara dados pessoais e preserva metricas
 * curtas do dominio.
 */
class AnonimizadorRegexTest {

    private final AnonimizadorRegex anonimizador = new AnonimizadorRegex();

    @Test
    @DisplayName("Mascara e-mail, CPF e telefone")
    void mascaraDadosPessoais() {
        String texto = "Paciente ana.sousa@emserh.ma.gov.br, CPF 123.456.789-00, tel (98) 98765-4321.";
        String anonimo = anonimizador.anonimizar(texto);
        assertThat(anonimo).doesNotContain("ana.sousa@emserh.ma.gov.br");
        assertThat(anonimo).doesNotContain("123.456.789-00");
        assertThat(anonimo).doesNotContain("98765-4321");
        assertThat(anonimo).contains("[email]").contains("[cpf]").contains("[telefone]");
    }

    @Test
    @DisplayName("Mascara CNPJ e sequencias longas de digitos")
    void mascaraCnpjEDigitos() {
        assertThat(anonimizador.anonimizar("Fornecedor 12.345.678/0001-90")).contains("[cnpj]");
        // 8 digitos: sem forma de telefone (que exige 10-11), cai no marcador generico de numero.
        assertThat(anonimizador.anonimizar("Registro 12345678")).contains("[numero]");
    }

    @Test
    @DisplayName("Preserva metricas curtas do dominio (estoque, cobertura)")
    void preservaMetricas() {
        String texto = "Estoque de 1500 unidades, cobertura de 12 dias, MAPE 8%.";
        assertThat(anonimizador.anonimizar(texto)).isEqualTo(texto);
    }

    @Test
    @DisplayName("Texto nulo ou vazio passa intacto")
    void nuloOuVazio() {
        assertThat(anonimizador.anonimizar(null)).isNull();
        assertThat(anonimizador.anonimizar("  ")).isEqualTo("  ");
    }
}
