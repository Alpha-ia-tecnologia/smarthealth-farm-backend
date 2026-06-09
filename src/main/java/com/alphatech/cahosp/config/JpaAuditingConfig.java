package com.alphatech.cahosp.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Habilita o JPA Auditing ({@code @CreatedDate}/{@code @LastModifiedDate}).
 *
 * <p>Fica isolado aqui (e nao na classe principal) para que os slices web de teste
 * ({@code @WebMvcTest}) nao tentem inicializar auditoria sem infraestrutura JPA.
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
