package br.com.postech.hospital.scheduling.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidacao(MethodArgumentNotValidException ex) {
        List<String> detalhes = ex.getBindingResult().getFieldErrors().stream()
                .map(erro -> erro.getField() + ": " + erro.getDefaultMessage())
                .toList();
        return ResponseEntity.badRequest()
                .body(ErrorResponse.de(400, "Dados inválidos", "A requisição contém campos inválidos", detalhes));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNaoEncontrado(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.de(404, "Recurso não encontrado", ex.getMessage()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAcessoNegado(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ErrorResponse.de(403, "Acesso negado", "Você não tem permissão para acessar este recurso"));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleCredenciaisInvalidas(BadCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ErrorResponse.de(401, "Credenciais inválidas", "Usuário ou senha incorretos"));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleArgumentoInvalido(IllegalArgumentException ex) {
        return ResponseEntity.badRequest()
                .body(ErrorResponse.de(400, "Requisição inválida", ex.getMessage()));
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
        return ResponseEntity.badRequest()
                .body(ErrorResponse.de(400, "Requisição inválida",
                        "A operação viola uma restrição de integridade dos dados"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleErroInesperado(Exception ex) {
        log.error("Erro inesperado ao processar requisição", ex);
        return ResponseEntity.internalServerError()
                .body(ErrorResponse.de(500, "Erro interno", "Ocorreu um erro inesperado. Tente novamente."));
    }
}
