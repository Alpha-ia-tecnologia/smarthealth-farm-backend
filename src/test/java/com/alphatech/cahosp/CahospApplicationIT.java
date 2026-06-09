package com.alphatech.cahosp;

import com.alphatech.cahosp.suporte.BaseIntegracaoPostgres;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Smoke de integracao da Fase 0: o contexto Spring sobe por completo contra um
 * PostgreSQL real (Testcontainers) e as migrations Flyway sao aplicadas.
 */
class CahospApplicationIT extends BaseIntegracaoPostgres {

    @Test
    @DisplayName("O contexto da aplicacao carrega com PostgreSQL real")
    void contextoCarrega() {
        // Sem assercoes: o teste falha se o contexto nao subir (datasource, JPA, Flyway, security).
    }
}
