package br.com.postech.hospital.history.historico;

import br.com.postech.hospital.events.ConsultaEvent;
import br.com.postech.hospital.events.StatusConsulta;
import br.com.postech.hospital.events.TipoEventoConsulta;
import br.com.postech.hospital.security.AuthenticatedUser;
import br.com.postech.hospital.security.SecurityRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HistoricoServiceTest {

    @Mock
    private ConsultaHistoricoRepository repository;

    private HistoricoService historicoService;

    private final UUID consultaId = UUID.randomUUID();
    private final UUID pacienteId = UUID.randomUUID();
    private final UUID medicoId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        historicoService = new HistoricoService(repository);
    }

    @Test
    void aplicarDeveCriarRegistroQuandoNaoExisteHistoricoParaAConsulta() {
        when(repository.findById(consultaId)).thenReturn(Optional.empty());
        ConsultaEvent evento = ConsultaEvent.criada(consultaId, pacienteId, medicoId, LocalDateTime.now().plusDays(1), StatusConsulta.AGENDADA);

        historicoService.aplicar(evento);

        ArgumentCaptor<ConsultaHistorico> captor = ArgumentCaptor.forClass(ConsultaHistorico.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getConsultaId()).isEqualTo(consultaId);
        assertThat(captor.getValue().getStatus()).isEqualTo(StatusConsulta.AGENDADA);
    }

    @Test
    void aplicarDeveAtualizarQuandoEventoEhMaisRecenteQueOHistoricoExistente() {
        LocalDateTime dataAntiga = LocalDateTime.now().minusDays(1);
        ConsultaHistorico existente = new ConsultaHistorico(consultaId, pacienteId, medicoId,
                LocalDateTime.now().plusDays(1), StatusConsulta.AGENDADA, dataAntiga);
        when(repository.findById(consultaId)).thenReturn(Optional.of(existente));

        ConsultaEvent eventoMaisNovo = ConsultaEvent.editada(consultaId, pacienteId, medicoId,
                LocalDateTime.now().plusDays(2), StatusConsulta.REALIZADA);

        historicoService.aplicar(eventoMaisNovo);

        assertThat(existente.getStatus()).isEqualTo(StatusConsulta.REALIZADA);
        verify(repository).save(existente);
    }

    @Test
    void aplicarDeveIgnorarEventoDesatualizadoOuDuplicado() {
        LocalDateTime agora = LocalDateTime.now();
        ConsultaHistorico existente = new ConsultaHistorico(consultaId, pacienteId, medicoId,
                LocalDateTime.now().plusDays(1), StatusConsulta.AGENDADA, agora);
        when(repository.findById(consultaId)).thenReturn(Optional.of(existente));

        ConsultaEvent eventoAntigo = new ConsultaEvent(UUID.randomUUID(), TipoEventoConsulta.EDITADA,
                consultaId, pacienteId, medicoId, LocalDateTime.now().plusDays(1), StatusConsulta.AGENDADA, agora.minusMinutes(1));

        historicoService.aplicar(eventoAntigo);

        assertThat(existente.getStatus()).isEqualTo(StatusConsulta.AGENDADA);
        verify(repository, never()).save(existente);
    }

    @Test
    void consultasDoPacienteDevePermitirMedicoConsultarQualquerPaciente() {
        AuthenticatedUser medico = new AuthenticatedUser(UUID.randomUUID(), "dra.ana", SecurityRole.MEDICO);
        when(repository.findByPacienteId(pacienteId)).thenReturn(List.of());

        List<ConsultaHistoricoResponse> resultado = historicoService.consultasDoPaciente(pacienteId, medico);

        assertThat(resultado).isEmpty();
        verify(repository).findByPacienteId(pacienteId);
    }

    @Test
    void consultasDoPacienteDeveNegarAcessoQuandoPacienteConsultaOutroPaciente() {
        AuthenticatedUser outroPaciente = new AuthenticatedUser(UUID.randomUUID(), "paciente.maria", SecurityRole.PACIENTE);

        assertThatThrownBy(() -> historicoService.consultasDoPaciente(pacienteId, outroPaciente))
                .isInstanceOf(AccessDeniedException.class);
        verify(repository, never()).findByPacienteId(any());
    }

    @Test
    void consultasFuturasDoPacienteDevePermitirOProprioPaciente() {
        AuthenticatedUser paciente = new AuthenticatedUser(pacienteId, "paciente.joao", SecurityRole.PACIENTE);
        when(repository.findByPacienteIdAndDataHoraAfter(eq(pacienteId), any())).thenReturn(List.of());

        List<ConsultaHistoricoResponse> resultado = historicoService.consultasFuturasDoPaciente(pacienteId, paciente);

        assertThat(resultado).isEmpty();
    }
}
