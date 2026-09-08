package br.com.postech.hospital.notification.notificacao;

import br.com.postech.hospital.security.AuthenticatedUser;
import br.com.postech.hospital.security.SecurityRole;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificacaoControllerTest {

    @Mock
    private NotificacaoService notificacaoService;

    private NotificacaoController controller;
    private final AuthenticatedUser usuarioLogado = new AuthenticatedUser(UUID.randomUUID(), "paciente.joao", SecurityRole.PACIENTE);

    @BeforeEach
    void setUp() {
        controller = new NotificacaoController(notificacaoService);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(usuarioLogado, null, List.of()));
    }

    @AfterEach
    void limparContexto() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void listarDeveDelegarParaOServicoComOUsuarioAutenticado() {
        UUID pacienteId = UUID.randomUUID();
        when(notificacaoService.listar(pacienteId, usuarioLogado)).thenReturn(List.of());

        ResponseEntity<List<NotificacaoResponse>> response = controller.listar(pacienteId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(notificacaoService).listar(pacienteId, usuarioLogado);
    }
}
