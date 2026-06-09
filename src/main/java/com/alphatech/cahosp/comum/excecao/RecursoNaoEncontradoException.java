package com.alphatech.cahosp.comum.excecao;

/**
 * Lancada quando um recurso solicitado nao existe. Mapeada para HTTP 404.
 */
public class RecursoNaoEncontradoException extends RuntimeException {

    public RecursoNaoEncontradoException(String mensagem) {
        super(mensagem);
    }
}
