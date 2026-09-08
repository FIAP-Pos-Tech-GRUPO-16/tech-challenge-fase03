package br.com.postech.hospital.history.graphql;

import br.com.postech.hospital.history.historico.HistoricoService;
import br.com.postech.hospital.security.AuthenticatedUser;
import br.com.postech.hospital.security.SecurityRole;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HistoricoGraphQlControllerTest {

    @Mock
    private HistoricoService historicoService;

    private HistoricoGraphQlController controller;

    @BeforeEach
    void setUp() {
        controller = new HistoricoGraphQlController(historicoService);
    }

    @AfterEach
    void limparContexto() {
        SecurityContextHolder.clearContext();
    }

    private void autenticarComo(AuthenticatedUser usuario) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(usuario, null, List.of()));
    }

    @Test
    void consultasPorPacienteDeveDelegarParaOServicoComOUsuarioAutenticado() {
        AuthenticatedUser medico = new AuthenticatedUser(UUID.randomUUID(), "dra.ana", SecurityRole.MEDICO);
        autenticarComo(medico);
        UUID pacienteId = UUID.randomUUID();
        when(historicoService.consultasDoPaciente(pacienteId, medico)).thenReturn(List.of());

        controller.consultasPorPaciente(pacienteId);

        verify(historicoService).consultasDoPaciente(pacienteId, medico);
    }

    @Test
    void consultasFuturasPorPacienteDeveDelegarParaOServico() {
        AuthenticatedUser enfermeiro = new AuthenticatedUser(UUID.randomUUID(), "enf.bruno", SecurityRole.ENFERMEIRO);
        autenticarComo(enfermeiro);
        UUID pacienteId = UUID.randomUUID();
        when(historicoService.consultasFuturasDoPaciente(pacienteId, enfermeiro)).thenReturn(List.of());

        controller.consultasFuturasPorPaciente(pacienteId);

        verify(historicoService).consultasFuturasDoPaciente(pacienteId, enfermeiro);
    }

    @Test
    void minhasConsultasDevePermitirPacienteEUsarOProprioId() {
        AuthenticatedUser paciente = new AuthenticatedUser(UUID.randomUUID(), "paciente.joao", SecurityRole.PACIENTE);
        autenticarComo(paciente);
        when(historicoService.consultasDoPaciente(paciente.id(), paciente)).thenReturn(List.of());

        controller.minhasConsultas();

        verify(historicoService).consultasDoPaciente(paciente.id(), paciente);
    }

    @Test
    void minhasConsultasDeveNegarAcessoParaMedico() {
        AuthenticatedUser medico = new AuthenticatedUser(UUID.randomUUID(), "dra.ana", SecurityRole.MEDICO);
        autenticarComo(medico);

        assertThatThrownBy(controller::minhasConsultas).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void minhasConsultasFuturasDevePermitirPacienteEUsarOProprioId() {
        AuthenticatedUser paciente = new AuthenticatedUser(UUID.randomUUID(), "paciente.joao", SecurityRole.PACIENTE);
        autenticarComo(paciente);
        when(historicoService.consultasFuturasDoPaciente(paciente.id(), paciente)).thenReturn(List.of());

        controller.minhasConsultasFuturas();

        verify(historicoService).consultasFuturasDoPaciente(paciente.id(), paciente);
    }

    @Test
    void minhasConsultasFuturasDeveNegarAcessoParaEnfermeiro() {
        AuthenticatedUser enfermeiro = new AuthenticatedUser(UUID.randomUUID(), "enf.bruno", SecurityRole.ENFERMEIRO);
        autenticarComo(enfermeiro);

        assertThatThrownBy(controller::minhasConsultasFuturas).isInstanceOf(AccessDeniedException.class);
    }
}
