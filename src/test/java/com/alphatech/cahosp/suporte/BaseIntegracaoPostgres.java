package com.alphatech.cahosp.suporte;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base dos testes de integracao ({@code *IT}). Sobe um PostgreSQL real via Testcontainers
 * (requer Docker), aplica as migrations Flyway e expoe o contexto Spring completo.
 *
 * <p><strong>Padrao singleton:</strong> o container e iniciado UMA vez no bloco estatico e
 * nunca e parado manualmente (o Ryuk do Testcontainers o remove ao fim da JVM). Nao usamos
 * {@code @Testcontainers}/{@code @Container} de proposito: aquele ciclo para o container ao
 * fim de cada classe, e como o Spring reaproveita o contexto em cache (via
 * {@link ServiceConnection}), as classes seguintes apontariam para um container morto
 * (timeout de 30s no HikariCP). Com o singleton, todos os contextos compartilham um unico
 * container vivo durante toda a execucao.
 */
@SpringBootTest
@ActiveProfiles("test")
public abstract class BaseIntegracaoPostgres {

    // Mesma major version do ambiente (PostgreSQL 17) para que os *IT validem o comportamento real
    // do banco de producao — divergencias de versao (ex.: inferencia de tipo de parametro) ja
    // morderam aqui. Ver [[convencoes-enum-e-query]].
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine");

    static {
        POSTGRES.start();
    }
}
