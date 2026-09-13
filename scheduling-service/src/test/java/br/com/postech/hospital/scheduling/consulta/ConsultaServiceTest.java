package br.com.postech.hospital.scheduling.consulta;

import br.com.postech.hospital.events.StatusConsulta;
import br.com.postech.hospital.scheduling.exception.ResourceNotFoundException;
import br.com.postech.hospital.scheduling.messaging.ConsultaAlteradaEvent;
import br.com.postech.hospital.scheduling.usuario.Usuario;
import br.com.postech.hospital.scheduling.usuario.UsuarioRepository;
import br.com.postech.hospital.security.AuthenticatedUser;
import br.com.postech.hospital.security.SecurityRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConsultaServiceTest {

    @Mock
    private ConsultaRepository consultaRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    private ConsultaService consultaService;

    private final UUID pacienteId = UUID.randomUUID();
    private final UUID medicoId = UUID.randomUUID();
    private final LocalDateTime dataHora = LocalDateTime.now().plusDays(1);

    @BeforeEach
    void setUp() {
        consultaService = new ConsultaService(consultaRepository, usuarioRepository, applicationEventPublisher);
    }

    private AuthenticatedUser medico() {
        return new AuthenticatedUser(medicoId, "dra.ana", SecurityRole.MEDICO);
    }

    private AuthenticatedUser enfermeiro() {
        return new AuthenticatedUser(UUID.randomUUID(), "enf.bruno", SecurityRole.ENFERMEIRO);
    }

    private AuthenticatedUser paciente(UUID id) {
        return new AuthenticatedUser(id, "paciente.joao", SecurityRole.PACIENTE);
    }

    /** A criacao passou a exigir que paciente e medico existam com o papel correto. */
    private void cadastrosValidos() {
        when(usuarioRepository.findById(pacienteId)).thenReturn(Optional.of(
                new Usuario(pacienteId, "Joao Pereira", "paciente.joao", "hash", SecurityRole.PACIENTE)));
        when(usuarioRepository.findById(medicoId)).thenReturn(Optional.of(
                new Usuario(medicoId, "Dra. Ana", "medica.ana", "hash", SecurityRole.MEDICO)));
    }

    @Test
    void criarDeveSalvarConsultaEPublicarEventoDeCriacao() {
        cadastrosValidos();
        ConsultaRequest request = new ConsultaRequest(pacienteId, medicoId, dataHora, "primeira consulta");

        ConsultaResponse response = consultaService.criar(request, enfermeiro());

        assertThat(response.pacienteId()).isEqualTo(pacienteId);
        assertThat(response.medicoId()).isEqualTo(medicoId);
        assertThat(response.status()).isEqualTo(StatusConsulta.AGENDADA);
        verify(consultaRepository).save(any(Consulta.class));

        ArgumentCaptor<ConsultaAlteradaEvent> captor = ArgumentCaptor.forClass(ConsultaAlteradaEvent.class);
        verify(applicationEventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().tipo().name()).isEqualTo("CRIADA");
    }

    @Test
    void editarDeveAtualizarConsultaExistenteEPublicarEventoDeEdicao() {
        Consulta consulta = Consulta.agendar(pacienteId, medicoId, medicoId, dataHora, "original");
        when(consultaRepository.findById(consulta.getId())).thenReturn(Optional.of(consulta));
        ConsultaUpdateRequest request = new ConsultaUpdateRequest(dataHora.plusHours(1), StatusConsulta.REALIZADA, "atualizada");

        ConsultaResponse response = consultaService.editar(consulta.getId(), request);

        assertThat(response.status()).isEqualTo(StatusConsulta.REALIZADA);
        assertThat(response.observacoes()).isEqualTo("atualizada");

        ArgumentCaptor<ConsultaAlteradaEvent> captor = ArgumentCaptor.forClass(ConsultaAlteradaEvent.class);
        verify(applicationEventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().tipo().name()).isEqualTo("EDITADA");
    }

    @Test
    void editarDeveLancarNotFoundQuandoConsultaNaoExiste() {
        UUID idInexistente = UUID.randomUUID();
        when(consultaRepository.findById(idInexistente)).thenReturn(Optional.empty());
        ConsultaUpdateRequest request = new ConsultaUpdateRequest(dataHora, StatusConsulta.REALIZADA, null);

        assertThatThrownBy(() -> consultaService.editar(idInexistente, request))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(applicationEventPublisher, never()).publishEvent(any());
    }

    @Test
    void buscarPorIdDevePermitirMedicoVerQualquerConsulta() {
        Consulta consulta = Consulta.agendar(pacienteId, medicoId, medicoId, dataHora, null);
        when(consultaRepository.findById(consulta.getId())).thenReturn(Optional.of(consulta));

        ConsultaResponse response = consultaService.buscarPorId(consulta.getId(), medico());

        assertThat(response.id()).isEqualTo(consulta.getId());
    }

    @Test
    void buscarPorIdDevePermitirPacienteVerAPropriaConsulta() {
        Consulta consulta = Consulta.agendar(pacienteId, medicoId, medicoId, dataHora, null);
        when(consultaRepository.findById(consulta.getId())).thenReturn(Optional.of(consulta));

        ConsultaResponse response = consultaService.buscarPorId(consulta.getId(), paciente(pacienteId));

        assertThat(response.id()).isEqualTo(consulta.getId());
    }

    @Test
    void buscarPorIdDeveNegarAcessoQuandoPacienteTentaVerConsultaDeOutroPaciente() {
        Consulta consulta = Consulta.agendar(pacienteId, medicoId, medicoId, dataHora, null);
        when(consultaRepository.findById(consulta.getId())).thenReturn(Optional.of(consulta));
        AuthenticatedUser outroPaciente = paciente(UUID.randomUUID());

        assertThatThrownBy(() -> consultaService.buscarPorId(consulta.getId(), outroPaciente))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void buscarPorIdDeveLancarNotFoundQuandoConsultaNaoExiste() {
        UUID idInexistente = UUID.randomUUID();
        when(consultaRepository.findById(idInexistente)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> consultaService.buscarPorId(idInexistente, medico()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void listarDeveIgnorarFiltroInformadoQuandoAutorEhPaciente() {
        AuthenticatedUser paciente = paciente(pacienteId);
        when(consultaRepository.findByPacienteId(pacienteId)).thenReturn(List.of());

        consultaService.listar(UUID.randomUUID(), paciente);

        verify(consultaRepository).findByPacienteId(pacienteId);
        verify(consultaRepository, never()).findAll();
    }

    @Test
    void listarDeveFiltrarPorPacienteQuandoMedicoInformaFiltro() {
        when(consultaRepository.findByPacienteId(pacienteId)).thenReturn(List.of());

        consultaService.listar(pacienteId, medico());

        verify(consultaRepository).findByPacienteId(pacienteId);
        verify(consultaRepository, never()).findAll();
    }

    @Test
    void listarDeveRetornarTodasQuandoMedicoNaoInformaFiltro() {
        when(consultaRepository.findAll()).thenReturn(List.of());

        consultaService.listar(null, medico());

        verify(consultaRepository).findAll();
        verify(consultaRepository, never()).findByPacienteId(any());
    }

    @Test
    void criarDeveRecusarPacienteInexistente() {
        when(usuarioRepository.findById(pacienteId)).thenReturn(Optional.empty());
        ConsultaRequest request = new ConsultaRequest(pacienteId, medicoId, dataHora, null);

        assertThatThrownBy(() -> consultaService.criar(request, enfermeiro()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("pacienteId");
        verify(consultaRepository, never()).save(any());
        verify(applicationEventPublisher, never()).publishEvent(any());
    }

    @Test
    void criarDeveRecusarMedicoInexistente() {
        when(usuarioRepository.findById(pacienteId)).thenReturn(Optional.of(
                new Usuario(pacienteId, "Joao Pereira", "paciente.joao", "hash", SecurityRole.PACIENTE)));
        when(usuarioRepository.findById(medicoId)).thenReturn(Optional.empty());
        ConsultaRequest request = new ConsultaRequest(pacienteId, medicoId, dataHora, null);

        assertThatThrownBy(() -> consultaService.criar(request, enfermeiro()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("medicoId");
        verify(consultaRepository, never()).save(any());
    }

    @Test
    void criarDeveRecusarQuandoOMedicoInformadoNaVerdadeEhUmPaciente() {
        when(usuarioRepository.findById(pacienteId)).thenReturn(Optional.of(
                new Usuario(pacienteId, "Joao Pereira", "paciente.joao", "hash", SecurityRole.PACIENTE)));
        when(usuarioRepository.findById(medicoId)).thenReturn(Optional.of(
                new Usuario(medicoId, "Maria Santos", "paciente.maria", "hash", SecurityRole.PACIENTE)));
        ConsultaRequest request = new ConsultaRequest(pacienteId, medicoId, dataHora, null);

        assertThatThrownBy(() -> consultaService.criar(request, enfermeiro()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("medicoId");
        verify(consultaRepository, never()).save(any());
    }
}
