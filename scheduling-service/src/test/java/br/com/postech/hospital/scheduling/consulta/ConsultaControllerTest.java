package br.com.postech.hospital.scheduling.consulta;

import br.com.postech.hospital.events.StatusConsulta;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConsultaControllerTest {

    @Mock
    private ConsultaService consultaService;

    private ConsultaController controller;

    private final AuthenticatedUser usuarioLogado = new AuthenticatedUser(UUID.randomUUID(), "dra.ana", SecurityRole.MEDICO);

    @BeforeEach
    void setUp() {
        controller = new ConsultaController(consultaService);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(usuarioLogado, null, List.of()));
    }

    @AfterEach
    void limparContexto() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void criarDeveRetornar201ComOUsuarioAutenticadoComoAutor() {
        ConsultaRequest request = new ConsultaRequest(UUID.randomUUID(), UUID.randomUUID(), LocalDateTime.now().plusDays(1), null);
        ConsultaResponse resposta = new ConsultaResponse(UUID.randomUUID(), request.pacienteId(), request.medicoId(),
                usuarioLogado.id(), request.dataHora(), StatusConsulta.AGENDADA, null, LocalDateTime.now(), LocalDateTime.now());
        when(consultaService.criar(request, usuarioLogado)).thenReturn(resposta);

        ResponseEntity<ConsultaResponse> response = controller.criar(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo(resposta);
        verify(consultaService).criar(request, usuarioLogado);
    }

    @Test
    void editarDeveRetornar200ComOUsuarioAutenticadoComoAutor() {
        UUID id = UUID.randomUUID();
        ConsultaUpdateRequest request = new ConsultaUpdateRequest(LocalDateTime.now().plusDays(1), StatusConsulta.REALIZADA, null);
        ConsultaResponse resposta = new ConsultaResponse(id, UUID.randomUUID(), UUID.randomUUID(),
                usuarioLogado.id(), request.dataHora(), request.status(), null, LocalDateTime.now(), LocalDateTime.now());
        when(consultaService.editar(id, request, usuarioLogado)).thenReturn(resposta);

        ResponseEntity<ConsultaResponse> response = controller.editar(id, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(resposta);
    }

    @Test
    void buscarPorIdDeveDelegarParaOServicoComOUsuarioAutenticado() {
        UUID id = UUID.randomUUID();
        ConsultaResponse resposta = new ConsultaResponse(id, UUID.randomUUID(), UUID.randomUUID(),
                usuarioLogado.id(), LocalDateTime.now(), StatusConsulta.AGENDADA, null, LocalDateTime.now(), LocalDateTime.now());
        when(consultaService.buscarPorId(id, usuarioLogado)).thenReturn(resposta);

        ResponseEntity<ConsultaResponse> response = controller.buscarPorId(id);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(resposta);
    }

    @Test
    void listarDeveRepassarFiltroDePacienteAoServico() {
        UUID pacienteId = UUID.randomUUID();
        when(consultaService.listar(pacienteId, usuarioLogado)).thenReturn(List.of());

        ResponseEntity<List<ConsultaResponse>> response = controller.listar(pacienteId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(consultaService).listar(pacienteId, usuarioLogado);
    }
}
