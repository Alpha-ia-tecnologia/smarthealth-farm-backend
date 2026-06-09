package com.alphatech.cahosp.config;

import com.alphatech.cahosp.alerta.dominio.Severidade;
import com.alphatech.cahosp.alerta.dominio.StatusAlerta;
import com.alphatech.cahosp.alerta.dominio.TipoAlerta;
import com.alphatech.cahosp.estoque.dominio.StatusEstoque;
import com.alphatech.cahosp.estoque.dominio.TipoMovimentacao;
import com.alphatech.cahosp.medicamento.dominio.Criticidade;
import com.alphatech.cahosp.medicamento.dominio.FamiliaTerapeutica;
import com.alphatech.cahosp.previsao.dominio.Drift;
import com.alphatech.cahosp.unidade.dominio.Conectividade;
import com.alphatech.cahosp.unidade.dominio.Porte;
import com.alphatech.cahosp.usuario.dominio.Perfil;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Conversao de enums recebidos como query param (ex.: {@code ?familia=Antibióticos}).
 *
 * <p>O Spring converte enum por {@code name()} ({@code ANTIBIOTICOS}); o {@code @JsonCreator}
 * dos enums so vale para o corpo JSON. Aqui registramos conversores que delegam ao
 * {@code fromJson} de cada enum, aceitando tanto o nome quanto o rotulo pt-BR — alinhando o
 * comportamento dos filtros de listagem ao restante da API. RF-DAD-06 / RF-ADM.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(String.class, Perfil.class, Perfil::fromJson);
        registry.addConverter(String.class, FamiliaTerapeutica.class, FamiliaTerapeutica::fromJson);
        registry.addConverter(String.class, Criticidade.class, Criticidade::fromJson);
        registry.addConverter(String.class, Porte.class, Porte::fromJson);
        registry.addConverter(String.class, Conectividade.class, Conectividade::fromJson);
        registry.addConverter(String.class, StatusEstoque.class, StatusEstoque::fromJson);
        registry.addConverter(String.class, TipoMovimentacao.class, TipoMovimentacao::fromJson);
        registry.addConverter(String.class, Drift.class, Drift::fromJson);
        registry.addConverter(String.class, TipoAlerta.class, TipoAlerta::fromJson);
        registry.addConverter(String.class, Severidade.class, Severidade::fromJson);
        registry.addConverter(String.class, StatusAlerta.class, StatusAlerta::fromJson);
    }
}
