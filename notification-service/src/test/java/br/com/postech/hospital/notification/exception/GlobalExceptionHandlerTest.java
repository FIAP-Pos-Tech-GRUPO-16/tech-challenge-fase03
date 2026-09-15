package br.com.postech.hospital.notification.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

@DisplayName("Tratamento de erro do serviço de notificações")
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("pacienteId malformado no filtro -> 400 (era 401)")
    void tipoInvalidoDeveRetornar400() {
        MethodArgumentTypeMismatchException ex = mock(MethodArgumentTypeMismatchException.class);
        doReturn("pacienteId").when(ex).getName();
        doReturn(UUID.class).when(ex).getRequiredType();

        ResponseEntity<ErrorResponse> response = handler.handleTipoInvalido(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().message()).contains("pacienteId");
    }

    @Test
    @DisplayName("rota inexistente -> 404 (era 401)")
    void rotaInexistenteDeveRetornar404() {
        ResponseEntity<ErrorResponse> response =
                handler.handleRotaInexistente(new NoResourceFoundException(HttpMethod.GET, "/nao-existe"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("método não suportado -> 405 (era 401)")
    void metodoNaoSuportadoDeveRetornar405() {
        ResponseEntity<ErrorResponse> response =
                handler.handleMetodoNaoSuportado(new HttpRequestMethodNotSupportedException("POST"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
        assertThat(response.getBody().message()).contains("POST");
    }

    @Test
    @DisplayName("content-type não suportado -> 415")
    void mediaTypeNaoSuportadoDeveRetornar415() {
        ResponseEntity<ErrorResponse> response =
                handler.handleMediaTypeNaoSuportado(new HttpMediaTypeNotSupportedException("text/plain"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
    }

    @Test
    @DisplayName("JSON malformado -> 400")
    void jsonInvalidoDeveRetornar400() {
        HttpMessageNotReadableException ex =
                new HttpMessageNotReadableException("malformado", mock(HttpInputMessage.class));

        ResponseEntity<ErrorResponse> response = handler.handleJsonInvalido(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("acesso negado -> 403, sem repassar a mensagem interna")
    void acessoNegadoDeveRetornar403() {
        ResponseEntity<ErrorResponse> response = handler.handleAcessoNegado(new AccessDeniedException("detalhe interno"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().message()).doesNotContain("detalhe interno");
    }

    @Test
    @DisplayName("erro inesperado -> 500 sem vazar a causa")
    void erroInesperadoDeveRetornar500SemVazarCausa() {
        ResponseEntity<ErrorResponse> response =
                handler.handleErroInesperado(new RuntimeException("NullPointer na linha 42"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().message()).doesNotContain("NullPointer");
    }
}
