package com.alphatech.cahosp.comum;

import com.alphatech.cahosp.comum.excecao.ConflitoException;
import com.alphatech.cahosp.comum.excecao.RecursoNaoEncontradoException;
import com.alphatech.cahosp.comum.excecao.RegraNegocioException;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.stream.Collectors;

/**
 * Tratamento global de erros: toda excecao vira o envelope {@link ApiResponse} de erro,
 * com o status HTTP e o {@code codigo} corretos. RF-SEG (respostas consistentes).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ApiResponse<Void>> tratarNaoEncontrado(RecursoNaoEncontradoException ex) {
        return resposta(HttpStatus.NOT_FOUND, ex.getMessage(), "NAO_ENCONTRADO");
    }

    @ExceptionHandler(ConflitoException.class)
    public ResponseEntity<ApiResponse<Void>> tratarConflito(ConflitoException ex) {
        return resposta(HttpStatus.CONFLICT, ex.getMessage(), "CONFLITO");
    }

    @ExceptionHandler(RegraNegocioException.class)
    public ResponseEntity<ApiResponse<Void>> tratarRegraNegocio(RegraNegocioException ex) {
        return resposta(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), "REGRA_NEGOCIO");
    }

    /** Falha de validacao de body (@Valid em DTO). */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> tratarValidacao(MethodArgumentNotValidException ex) {
        String mensagem = ex.getBindingResult().getFieldErrors().stream()
                .map(this::descreverCampo)
                .collect(Collectors.joining("; "));
        return resposta(HttpStatus.BAD_REQUEST, mensagem, "VALIDACAO");
    }

    /**
     * Corpo da requisicao ilegivel ou semanticamente invalido (JSON malformado, valor de enum
     * desconhecido recusado pelo {@code @JsonCreator}, tipo incompativel). E erro do cliente (400).
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> tratarCorpoInvalido(HttpMessageNotReadableException ex) {
        return resposta(HttpStatus.BAD_REQUEST,
                "Corpo da requisicao invalido ou mal formatado.", "VALIDACAO");
    }

    /** Parametro de path/query com tipo invalido (ex.: ?perfil=Inexistente, id nao-UUID). */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> tratarTipoInvalido(MethodArgumentTypeMismatchException ex) {
        String mensagem = "Valor invalido para o parametro '" + ex.getName() + "'.";
        return resposta(HttpStatus.BAD_REQUEST, mensagem, "VALIDACAO");
    }

    /** Falha de validacao em parametros (@Validated em path/query). */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> tratarConstraint(ConstraintViolationException ex) {
        String mensagem = ex.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .collect(Collectors.joining("; "));
        return resposta(HttpStatus.BAD_REQUEST, mensagem, "VALIDACAO");
    }

    /** Credenciais invalidas no login. */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> tratarCredenciais(BadCredentialsException ex) {
        return resposta(HttpStatus.UNAUTHORIZED, "E-mail ou senha invalidos.", "CREDENCIAIS_INVALIDAS");
    }

    /**
     * Acesso negado por RBAC (ex.: {@code @PreAuthorize} lanca {@code AuthorizationDeniedException},
     * subtipo de {@link AccessDeniedException}, durante o dispatch MVC). Mantem o mesmo envelope
     * {@code ACESSO_NEGADO} do {@code HandlerAcessoNegado}. RF-SEG (RBAC).
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> tratarAcessoNegado(AccessDeniedException ex) {
        return resposta(HttpStatus.FORBIDDEN, "Acesso negado para o seu perfil.", "ACESSO_NEGADO");
    }

    /** Demais falhas de autenticacao. */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> tratarAutenticacao(AuthenticationException ex) {
        return resposta(HttpStatus.UNAUTHORIZED, "Nao autenticado.", "NAO_AUTENTICADO");
    }

    /** Rede de seguranca: erro inesperado nao vaza stack trace para o cliente. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> tratarInesperado(Exception ex) {
        log.error("Erro inesperado", ex);
        return resposta(HttpStatus.INTERNAL_SERVER_ERROR,
                "Erro interno inesperado. Tente novamente.", "ERRO_INTERNO");
    }

    private String descreverCampo(FieldError erro) {
        return erro.getField() + ": " + erro.getDefaultMessage();
    }

    private ResponseEntity<ApiResponse<Void>> resposta(HttpStatus status, String mensagem, String codigo) {
        return ResponseEntity.status(status).body(ApiResponse.erro(mensagem, codigo));
    }
}
