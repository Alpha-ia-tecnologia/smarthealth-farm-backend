package com.alphatech.cahosp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Ponto de entrada da API Smart Health CAHOSP.
 *
 * <p>Gestao preditiva da cadeia farmaceutica da Central de Abastecimento Hospitalar
 * (CAHOSP / EMSERH-MA). Cobre os 62 requisitos funcionais (RF-*) do Edital FAPEMA GovIA.
 *
 * <p>O JPA Auditing fica em {@code config.JpaAuditingConfig} (e nao aqui) para nao
 * exigir infraestrutura JPA nos slices de teste web ({@code @WebMvcTest}).
 */
@SpringBootApplication
public class CahospApplication {

    public static void main(String[] args) {
        SpringApplication.run(CahospApplication.class, args);
    }
}
