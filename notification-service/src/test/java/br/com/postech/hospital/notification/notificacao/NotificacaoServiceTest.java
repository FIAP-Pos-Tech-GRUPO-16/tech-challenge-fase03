package br.com.postech.hospital.notification.notificacao;

import br.com.postech.hospital.events.ConsultaEvent;
import br.com.postech.hospital.events.StatusConsulta;
import br.com.postech.hospital.security.AuthenticatedUser;
import br.com.postech.hospital.security.SecurityRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificacaoServiceTest {

    @Mock
    private NotificacaoRepository notificacaoRepository;

    private NotificacaoService notificacaoService;

    private final UUID pacienteId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        notificacaoService = new NotificacaoService(notificacaoRepository);
    }

    private ConsultaEvent eventoCriado() {
        return ConsultaEvent.criada(UUID.randomUUID(), pacienteId, UUID.randomUUID(),
                LocalDateTime.now().plusDays(1), StatusConsulta.AGENDADA);
    }

    private ConsultaEvent eventoEditado() {
        return ConsultaEvent.editada(UUID.randomUUID(), pacienteId, UUID.randomUUID(),
                LocalDateTime.now().plusDays(2), StatusConsulta.AGENDADA);
    }

    @Test
    void processarDeveSalvarNotificacaoQuandoEventoAindaNaoFoiProcessado() {
        ConsultaEvent evento = eventoCriado();
        when(notificacaoRepository.existsByEventoId(evento.eventoId())).thenReturn(false);

        notificacaoService.processar(evento);

        ArgumentCaptor<Notificacao> captor = ArgumentCaptor.forClass(Notificacao.class);
        verify(notificacaoRepository).save(captor.capture());
        assertThat(captor.getValue().getEventoId()).isEqualTo(evento.eventoId());
        assertThat(captor.getValue().getPacienteId()).isEqualTo(pacienteId);
    }

    @Test
    void processarNaoDeveSalvarQuandoEventoJaFoiProcessado() {
        ConsultaEvent evento = eventoCriado();
        when(notificacaoRepository.existsByEventoId(evento.eventoId())).thenReturn(true);

        notificacaoService.processar(evento);

        verify(notificacaoRepository, never()).save(any());
    }

    @Test
    void processarDeveIgnorarSilenciosamenteViolacaoDeConstraintPorProcessamentoConcorrente() {
        ConsultaEvent evento = eventoCriado();
        when(notificacaoRepository.existsByEventoId(evento.eventoId())).thenReturn(false);
        when(notificacaoRepository.save(any())).thenThrow(new DataIntegrityViolationException("duplicado"));

        org.assertj.core.api.Assertions.assertThatCode(() -> notificacaoService.processar(evento))
                .doesNotThrowAnyException();
    }

    @Test
    void processarDeveMontarMensagemDeAtualizacaoQuandoEventoEhDeEdicao() {
        ConsultaEvent evento = eventoEditado();
        when(notificacaoRepository.existsByEventoId(evento.eventoId())).thenReturn(false);

        notificacaoService.processar(evento);

        ArgumentCaptor<Notificacao> captor = ArgumentCaptor.forClass(Notificacao.class);
        verify(notificacaoRepository).save(captor.capture());
        assertThat(captor.getValue().getMensagem()).contains("atualizada");
    }

    @Test
    void listarDeveForcarFiltroPeloProprioIdQuandoAutorEhPaciente() {
        AuthenticatedUser paciente = new AuthenticatedUser(pacienteId, "paciente.joao", SecurityRole.PACIENTE);
        when(notificacaoRepository.findByPacienteId(pacienteId)).thenReturn(List.of());

        notificacaoService.listar(UUID.randomUUID(), paciente);

        verify(notificacaoRepository).findByPacienteId(pacienteId);
        verify(notificacaoRepository, never()).findAll();
    }

    @Test
    void listarDeveRetornarTodasQuandoEnfermeiroNaoInformaFiltro() {
        AuthenticatedUser enfermeiro = new AuthenticatedUser(UUID.randomUUID(), "enf.bruno", SecurityRole.ENFERMEIRO);
        when(notificacaoRepository.findAll()).thenReturn(List.of());

        notificacaoService.listar(null, enfermeiro);

        verify(notificacaoRepository).findAll();
        verify(notificacaoRepository, never()).findByPacienteId(any());
    }

    @Test
    void listarDeveAplicarFiltroInformadoQuandoAutorEhMedicoOuEnfermeiro() {
        AuthenticatedUser medico = new AuthenticatedUser(UUID.randomUUID(), "dr.ana", SecurityRole.MEDICO);
        when(notificacaoRepository.findByPacienteId(pacienteId)).thenReturn(List.of());

        notificacaoService.listar(pacienteId, medico);

        verify(notificacaoRepository).findByPacienteId(pacienteId);
        verify(notificacaoRepository, never()).findAll();
    }

    @Test
    void listarDeveMapearNotificacoesEncontradasParaResponse() {
        AuthenticatedUser paciente = new AuthenticatedUser(pacienteId, "paciente.joao", SecurityRole.PACIENTE);
        Notificacao notificacao = Notificacao.paraLembreteDeConsulta(
                UUID.randomUUID(), UUID.randomUUID(), pacienteId, "Sua consulta foi agendada.");
        when(notificacaoRepository.findByPacienteId(pacienteId)).thenReturn(List.of(notificacao));

        List<NotificacaoResponse> resultado = notificacaoService.listar(null, paciente);

        assertThat(resultado).hasSize(1);
        NotificacaoResponse response = resultado.get(0);
        assertThat(response.id()).isEqualTo(notificacao.getId());
        assertThat(response.consultaId()).isEqualTo(notificacao.getConsultaId());
        assertThat(response.pacienteId()).isEqualTo(pacienteId);
        assertThat(response.canal()).isEqualTo(notificacao.getCanal());
        assertThat(response.mensagem()).isEqualTo(notificacao.getMensagem());
        assertThat(response.enviadaEm()).isEqualTo(notificacao.getEnviadaEm());
    }
}
