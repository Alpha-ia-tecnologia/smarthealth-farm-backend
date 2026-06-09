package com.alphatech.cahosp.comum.excecao;

/**
 * Lancada quando uma regra de negocio e violada (entrada semanticamente invalida).
 * Mapeada para HTTP 422.
 */
public class RegraNegocioException extends RuntimeException {

    public RegraNegocioException(String mensagem) {
        super(mensagem);
    }
}
