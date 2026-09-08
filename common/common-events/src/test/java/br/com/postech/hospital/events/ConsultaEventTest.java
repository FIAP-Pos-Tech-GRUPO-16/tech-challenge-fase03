package br.com.postech.hospital.events;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConsultaEventTest {

    private final UUID consultaId = UUID.randomUUID();
    private final UUID pacienteId = UUID.randomUUID();
    private final UUID medicoId = UUID.randomUUID();
    private final LocalDateTime dataHora = LocalDateTime.now().plusDays(1);

    @Test
    void deveCriarEventoDeCriacaoComTipoEIdDeEventoUnico() {
        ConsultaEvent evento = ConsultaEvent.criada(consultaId, pacienteId, medicoId, dataHora, StatusConsulta.AGENDADA);

        assertThat(evento.tipo()).isEqualTo(TipoEventoConsulta.CRIADA);
        assertThat(evento.consultaId()).isEqualTo(consultaId);
        assertThat(evento.pacienteId()).isEqualTo(pacienteId);
        assertThat(evento.medicoId()).isEqualTo(medicoId);
        assertThat(evento.dataHora()).isEqualTo(dataHora);
        assertThat(evento.status()).isEqualTo(StatusConsulta.AGENDADA);
        assertThat(evento.eventoId()).isNotNull();
        assertThat(evento.ocorridoEm()).isNotNull();
    }

    @Test
    void deveCriarEventoDeEdicaoComTipoCorreto() {
        ConsultaEvent evento = ConsultaEvent.editada(consultaId, pacienteId, medicoId, dataHora, StatusConsulta.REALIZADA);

        assertThat(evento.tipo()).isEqualTo(TipoEventoConsulta.EDITADA);
        assertThat(evento.status()).isEqualTo(StatusConsulta.REALIZADA);
    }

    @Test
    void doisEventosCriadosParaAMesmaConsultaDevemTerIdsDeEventoDiferentes() {
        ConsultaEvent primeiro = ConsultaEvent.criada(consultaId, pacienteId, medicoId, dataHora, StatusConsulta.AGENDADA);
        ConsultaEvent segundo = ConsultaEvent.criada(consultaId, pacienteId, medicoId, dataHora, StatusConsulta.AGENDADA);

        assertThat(primeiro.eventoId()).isNotEqualTo(segundo.eventoId());
    }

    @Test
    void deveRejeitarConstrucaoComCampoObrigatorioNulo() {
        assertThatThrownBy(() -> new ConsultaEvent(null, TipoEventoConsulta.CRIADA, consultaId,
                pacienteId, medicoId, dataHora, StatusConsulta.AGENDADA, LocalDateTime.now()))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("eventoId");

        assertThatThrownBy(() -> new ConsultaEvent(UUID.randomUUID(), null, consultaId,
                pacienteId, medicoId, dataHora, StatusConsulta.AGENDADA, LocalDateTime.now()))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("tipo");

        assertThatThrownBy(() -> new ConsultaEvent(UUID.randomUUID(), TipoEventoConsulta.CRIADA, consultaId,
                pacienteId, medicoId, dataHora, StatusConsulta.AGENDADA, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("ocorridoEm");
    }
}
