package br.com.postech.hospital.notification.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTipoInvalido(MethodArgumentTypeMismatchException ex) {
        String tipoEsperado = ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "válido";
        return ResponseEntity.badRequest()
                .body(ErrorResponse.de(400, "Requisição inválida",
                        "O parâmetro '%s' deve ser um %s".formatted(ex.getName(), tipoEsperado)));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleJsonInvalido(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest()
                .body(ErrorResponse.de(400, "Requisição inválida",
                        "O corpo da requisição deve conter JSON válido em UTF-8"));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMediaTypeNaoSuportado(HttpMediaTypeNotSupportedException ex) {
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(ErrorResponse.de(415, "Formato não suportado", "Esta API aceita apenas application/json"));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMetodoNaoSuportado(HttpRequestMethodNotSupportedException ex) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ErrorResponse.de(405, "Método não permitido",
                        "O método %s não é aceito neste recurso".formatted(ex.getMethod())));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleRotaInexistente(NoResourceFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.de(404, "Recurso não encontrado", "A rota solicitada não existe"));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAcessoNegado(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ErrorResponse.de(403, "Acesso negado", "Você não tem permissão para acessar este recurso"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleErroInesperado(Exception ex) {
        log.error("Erro inesperado ao processar requisição", ex);
        return ResponseEntity.internalServerError()
                .body(ErrorResponse.de(500, "Erro interno", "Ocorreu um erro inesperado. Tente novamente."));
    }
}
