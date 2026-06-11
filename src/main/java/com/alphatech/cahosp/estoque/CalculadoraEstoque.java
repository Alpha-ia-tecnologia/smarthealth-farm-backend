package com.alphatech.cahosp.estoque;

import com.alphatech.cahosp.estoque.dominio.StatusEstoque;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Regras puras de estoque (RF-EST-04/05) — sem dependencia de banco, faceis de testar.
 * Espelha {@code statusEstoque} e {@code diasAte} do front (src/data/index.ts).
 */
@Component
public class CalculadoraEstoque {

    /**
     * Margem de atencao acima do nivel critico (25%), conforme o front. Visivel no pacote para
     * que {@link EspecificacoesPosicao} aplique o mesmo limiar ao filtrar status na query (DRY).
     */
    static final double FATOR_ATENCAO = 1.25;

    /**
     * Situacao do estoque: {@code CRITICO} abaixo do nivel critico; {@code ATENCAO} ate 25%
     * acima dele; {@code OK} caso contrario. RF-EST-04.
     */
    public StatusEstoque status(int quantidade, int nivelCritico) {
        if (quantidade < nivelCritico) {
            return StatusEstoque.CRITICO;
        }
        if (quantidade < nivelCritico * FATOR_ATENCAO) {
            return StatusEstoque.ATENCAO;
        }
        return StatusEstoque.OK;
    }

    /** Dias entre a data de referencia e a validade (negativo = ja vencido). RF-EST-02. */
    public long diasParaVencer(LocalDate validade, LocalDate referencia) {
        return ChronoUnit.DAYS.between(referencia, validade);
    }

    /** Dias para vencer a partir de hoje. */
    public long diasParaVencer(LocalDate validade) {
        return diasParaVencer(validade, LocalDate.now());
    }
}
