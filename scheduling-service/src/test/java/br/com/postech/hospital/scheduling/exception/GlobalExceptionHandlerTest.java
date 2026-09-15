package br.com.postech.hospital.scheduling.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.http.HttpMethod;

import java.util.UUID;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleValidacaoDeveRetornar400ComDetalhesDosCampos() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(new FieldError("obj", "dataHora", "é obrigatório")));

        ResponseEntity<ErrorResponse> response = handler.handleValidacao(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().detalhes()).containsExactly("dataHora: é obrigatório");
    }

    @Test
    void handleNaoEncontradoDeveRetornar404() {
        ResponseEntity<ErrorResponse> response = handler.handleNaoEncontrado(new ResourceNotFoundException("não achou"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().message()).isEqualTo("não achou");
    }

    @Test
    void handleAcessoNegadoDeveRetornar403() {
        ResponseEntity<ErrorResponse> response = handler.handleAcessoNegado(new AccessDeniedException("negado"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void handleCredenciaisInvalidasDeveRetornar401() {
        ResponseEntity<ErrorResponse> response = handler.handleCredenciaisInvalidas(new BadCredentialsException("erro"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void handleArgumentoInvalidoDeveRetornar400() {
        ResponseEntity<ErrorResponse> response = handler.handleArgumentoInvalido(new IllegalArgumentException("argumento ruim"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().message()).isEqualTo("argumento ruim");
    }

    @Test
    void handleJsonInvalidoDeveRetornar400() {
        ResponseEntity<ErrorResponse> response = handler.handleJsonInvalido(
                new HttpMessageNotReadableException("JSON inválido"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().message()).contains("UTF-8");
    }

    @Test
    void handleErroInesperadoDeveRetornar500SemVazarDetalheInterno() {
        ResponseEntity<ErrorResponse> response = handler.handleErroInesperado(new RuntimeException("stacktrace sensível"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().message()).doesNotContain("stacktrace sensível");
    }

    @Test
    void uuidMalformadoNoPathDeveRetornar400ENaoo500() {
        MethodArgumentTypeMismatchException ex = mock(MethodArgumentTypeMismatchException.class);
        doReturn("id").when(ex).getName();
        doReturn(UUID.class).when(ex).getRequiredType();

        ResponseEntity<ErrorResponse> response = handler.handleTipoInvalido(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().message()).contains("id").contains("UUID");
    }

    @Test
    void contentTypeNaoSuportadoDeveRetornar415() {
        ResponseEntity<ErrorResponse> response =
                handler.handleMediaTypeNaoSuportado(new HttpMediaTypeNotSupportedException("text/plain"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        assertThat(response.getBody().status()).isEqualTo(415);
    }

    @Test
    void metodoHttpNaoSuportadoDeveRetornar405() {
        ResponseEntity<ErrorResponse> response =
                handler.handleMetodoNaoSuportado(new HttpRequestMethodNotSupportedException("DELETE"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
        assertThat(response.getBody().message()).contains("DELETE");
    }

    @Test
    void rotaInexistenteDeveRetornar404ENaoo500() {
        ResponseEntity<ErrorResponse> response =
                handler.handleRotaInexistente(new NoResourceFoundException(HttpMethod.GET, "/nao-existe"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().status()).isEqualTo(404);
    }
}
