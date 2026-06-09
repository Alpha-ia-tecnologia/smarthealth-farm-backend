package com.alphatech.cahosp.comum.excecao;

/**
 * Lancada quando uma operacao conflita com o estado atual (ex.: e-mail ja cadastrado).
 * Mapeada para HTTP 409.
 */
public class ConflitoException extends RuntimeException {

    public ConflitoException(String mensagem) {
        super(mensagem);
    }
}
