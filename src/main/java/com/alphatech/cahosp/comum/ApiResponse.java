package com.alphatech.cahosp.comum;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Collection;

/**
 * Envelope unico de resposta de toda a API.
 *
 * <p>As chaves ({@code success}/{@code data}/{@code error}/{@code codigo}) ficam em ingles
 * por convencao tecnica do wrapper consumido pelo frontend; o conteudo de {@code data} e as
 * mensagens sao em portugues. {@code total} so aparece em listas.
 *
 * <pre>
 * // sucesso
 * { "success": true, "data": &lt;payload&gt;, "total": 12 }
 * // erro
 * { "success": false, "error": "Mensagem clara em portugues", "codigo": "VALIDACAO" }
 * </pre>
 *
 * @param success indica sucesso (true) ou erro (false)
 * @param data    payload da resposta (null em erro)
 * @param total   total de itens (apenas em respostas de lista; null caso contrario)
 * @param error   mensagem de erro em portugues (null em sucesso)
 * @param codigo  codigo de erro estavel para o cliente (null em sucesso)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        boolean success,
        T data,
        Long total,
        String error,
        String codigo
) {

    /** Resposta de sucesso com payload, sem {@code total}. */
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null, null, null);
    }

    /** Resposta de sucesso para uma colecao, incluindo {@code total}. */
    public static <T extends Collection<?>> ApiResponse<T> lista(T data) {
        long total = data == null ? 0 : data.size();
        return new ApiResponse<>(true, data, total, null, null);
    }

    /** Resposta de erro com mensagem (pt-BR) e codigo estavel. */
    public static <T> ApiResponse<T> erro(String mensagem, String codigo) {
        return new ApiResponse<>(false, null, null, mensagem, codigo);
    }
}
