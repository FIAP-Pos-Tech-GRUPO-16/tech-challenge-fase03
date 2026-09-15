package br.com.postech.hospital.scheduling.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidacao(MethodArgumentNotValidException ex) {
        List<String> detalhes = ex.getBindingResult().getFieldErrors().stream().map(erro -> erro.getField() + ": " + erro.getDefaultMessage()).toList();
        return ResponseEntity.badRequest().body(ErrorResponse.de(400, "Dados inválidos", "A requisição contém campos inválidos", detalhes));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleJsonInvalido(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest().body(ErrorResponse.de(400, "Requisição inválida", "O corpo da requisição deve conter JSON válido em UTF-8"));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNaoEncontrado(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ErrorResponse.de(404, "Recurso não encontrado", ex.getMessage()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAcessoNegado(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ErrorResponse.de(403, "Acesso negado", "Você não tem permissão para acessar este recurso"));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleCredenciaisInvalidas(BadCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ErrorResponse.de(401, "Credenciais inválidas", "Usuário ou senha incorretos"));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleArgumentoInvalido(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(ErrorResponse.de(400, "Requisição inválida", ex.getMessage()));
    }

    /**
     * Rede de segurança: qualquer violação de restrição do banco (foreign key, unique, check)
     * é erro do dado que veio na requisição, não falha do servidor — então responde 400, e não
     * o 500 genérico do handler abaixo. A mensagem do banco não é repassada ao cliente, por não
     * expor detalhes de schema.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleViolacaoDeIntegridade(DataIntegrityViolationException ex) {
        log.warn("Requisição violou uma restrição de integridade: {}", ex.getMostSpecificCause().getMessage());
        return ResponseEntity.badRequest().body(ErrorResponse.de(400, "Requisição inválida", "A operação viola uma restrição de integridade dos dados"));
    }

    /**
     * Valor de path variable ou query param que não converte para o tipo esperado — um UUID
     * malformado em {@code /consultas/{id}}, por exemplo. É erro de quem chamou, não do servidor.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTipoInvalido(MethodArgumentTypeMismatchException ex) {
        String tipoEsperado = ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "válido";
        return ResponseEntity.badRequest().body(ErrorResponse.de(400, "Requisição inválida", "O parâmetro '%s' deve ser um %s".formatted(ex.getName(), tipoEsperado)));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMediaTypeNaoSuportado(HttpMediaTypeNotSupportedException ex) {
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(ErrorResponse.de(415, "Formato não suportado", "Esta API aceita apenas application/json"));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMetodoNaoSuportado(HttpRequestMethodNotSupportedException ex) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(ErrorResponse.de(405, "Método não permitido", "O método %s não é aceito neste recurso".formatted(ex.getMethod())));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleRotaInexistente(NoResourceFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ErrorResponse.de(404, "Recurso não encontrado", "A rota solicitada não existe"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleErroInesperado(Exception ex) {
        log.error("Erro inesperado ao processar requisição", ex);
        return ResponseEntity.internalServerError().body(ErrorResponse.de(500, "Erro interno", "Ocorreu um erro inesperado. Tente novamente."));
    }
}
