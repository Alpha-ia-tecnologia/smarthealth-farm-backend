package com.alphatech.cahosp.seguranca.auditoria.dominio;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

/**
 * Categoria da acao auditada (RF-SEG-02). Tipa o conjunto fechado de acoes sensiveis da plataforma,
 * permitindo filtrar a trilha por tipo de evento na revisao de conformidade — enquanto o front
 * exibe apenas a frase livre {@code acao}, no servidor a categoria torna o evento consultavel.
 *
 * <p>Cada categoria carrega valores-padrao ({@link #acaoPadrao()}, {@link #baseLegalPadrao()},
 * {@link #assistidoPorIaPadrao()}) usados pelo registrador quando o chamador nao os informa,
 * evitando repeticao nas dezenas de pontos de chamada.
 */
public enum CategoriaAuditoria {

    APROVAR_RECOMENDACAO("Aprovação de recomendação", "Aprovou recomendação",
            "Execução de contrato (art. 7º, V)", false),
    EXECUTAR_RECOMENDACAO("Execução de recomendação", "Executou recomendação",
            "Execução de contrato (art. 7º, V)", false),
    RECALIBRAR_PREVISAO("Recalibração de previsão", "Recalibrou modelo de previsão",
            "Execução de contrato", true),
    GERAR_ALERTAS("Geração de alertas", "Regenerou alertas pelo motor de regras",
            "Execução de contrato", false),
    ALTERAR_LIMIAR_ALERTA("Alteração de limiar de alerta", "Alterou limiar de alerta",
            "Execução de contrato", false),
    EXPORTAR_RELATORIO("Exportação de relatório", "Exportou relatório",
            "Legítimo interesse (art. 7º, IX)", false),
    INFERENCIA_IA("Inferência por IA", "Inferência via AI Gateway (dados anonimizados)",
            "Anonimização (art. 12)", true),
    GERIR_USUARIO("Gestão de usuário", "Operação sobre cadastro de usuário",
            "Execução de contrato", false),
    CONSULTAR("Consulta", "Consultou recurso",
            "Controle sanitário", false),
    AUTENTICAR("Autenticação", "Autenticação no sistema",
            "Execução de contrato", false);

    private final String rotulo;
    private final String acaoPadrao;
    private final String baseLegalPadrao;
    private final boolean assistidoPorIaPadrao;

    CategoriaAuditoria(String rotulo, String acaoPadrao, String baseLegalPadrao,
                       boolean assistidoPorIaPadrao) {
        this.rotulo = rotulo;
        this.acaoPadrao = acaoPadrao;
        this.baseLegalPadrao = baseLegalPadrao;
        this.assistidoPorIaPadrao = assistidoPorIaPadrao;
    }

    /** Rotulo exibido no frontend (pt-BR); tambem e o valor serializado em JSON. */
    @JsonValue
    public String rotulo() {
        return rotulo;
    }

    /** Frase de acao padrao quando o chamador nao detalha (ex.: "Aprovou recomendação"). */
    public String acaoPadrao() {
        return acaoPadrao;
    }

    /** Base legal LGPD padrao desta categoria (RF-SEG-03). */
    public String baseLegalPadrao() {
        return baseLegalPadrao;
    }

    /** Indica se, por natureza, a acao costuma ser assistida por IA (RF-SEG-02). */
    public boolean assistidoPorIaPadrao() {
        return assistidoPorIaPadrao;
    }

    /**
     * Desserializa do JSON/query-param, aceitando o nome da constante ("INFERENCIA_IA")
     * ou o rotulo pt-BR ("Inferência por IA"), ignorando maiusculas/minusculas.
     */
    @JsonCreator
    public static CategoriaAuditoria fromJson(String valor) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("Categoria da auditoria e obrigatoria.");
        }
        String alvo = valor.trim();
        return Arrays.stream(values())
                .filter(c -> c.name().equalsIgnoreCase(alvo) || c.rotulo.equalsIgnoreCase(alvo))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Categoria invalida: '" + valor + "'."));
    }
}
