package br.com.postech.hospital.scheduling.auth;

import br.com.postech.hospital.security.SecurityRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @Test
    void loginDeveRetornar200ComOTokenDoServico() {
        AuthController controller = new AuthController(authService);
        LoginRequest request = new LoginRequest("medica.ana", "Senha@123");
        LoginResponse resposta = new LoginResponse("token", "Bearer", SecurityRole.MEDICO, 120);
        when(authService.autenticar(request)).thenReturn(resposta);

        ResponseEntity<LoginResponse> response = controller.login(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(resposta);
    }
}
