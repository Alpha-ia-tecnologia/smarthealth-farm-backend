package com.alphatech.cahosp.ia;

/**
 * Anonimizacao de dados pessoais antes de qualquer envio a provedores de IA externos
 * (RF-SEG-04 / LGPD). Interface pequena e focada (segregacao de interface) para permitir
 * estrategias distintas e facilitar o teste.
 */
public interface Anonimizador {

    /** Substitui dados identificaveis (e-mail, CPF/CNPJ, telefone, numeros longos) por marcadores. */
    String anonimizar(String texto);
}
