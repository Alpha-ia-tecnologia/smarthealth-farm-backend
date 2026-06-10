package com.alphatech.cahosp.ia;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * Anonimizacao por expressoes regulares (RF-SEG-04): mascara e-mails, CPF, CNPJ, telefones e
 * sequencias longas de digitos antes do envio a IA externa. A ordem importa — os padroes mais
 * especificos (e formatados) sao aplicados antes do generico de digitos.
 *
 * <p>Conservador por design: prefere mascarar a mais (numeros longos viram {@code [numero]}) a
 * vazar dado pessoal. Metricas curtas do dominio (ex.: "1500 unidades", "12 dias") sao preservadas.
 */
@Component
public class AnonimizadorRegex implements Anonimizador {

    private static final Pattern EMAIL =
            Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}");
    private static final Pattern CNPJ =
            Pattern.compile("\\b\\d{2}\\.\\d{3}\\.\\d{3}/\\d{4}-\\d{2}\\b");
    private static final Pattern CPF =
            Pattern.compile("\\b\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2}\\b");
    private static final Pattern TELEFONE =
            Pattern.compile("\\(?\\d{2}\\)?[\\s-]?9?\\d{4}[\\s-]?\\d{4}");
    private static final Pattern DIGITOS_LONGOS =
            Pattern.compile("\\d{8,}");

    @Override
    public String anonimizar(String texto) {
        if (texto == null || texto.isBlank()) {
            return texto;
        }
        String resultado = EMAIL.matcher(texto).replaceAll("[email]");
        resultado = CNPJ.matcher(resultado).replaceAll("[cnpj]");
        resultado = CPF.matcher(resultado).replaceAll("[cpf]");
        resultado = TELEFONE.matcher(resultado).replaceAll("[telefone]");
        resultado = DIGITOS_LONGOS.matcher(resultado).replaceAll("[numero]");
        return resultado;
    }
}
