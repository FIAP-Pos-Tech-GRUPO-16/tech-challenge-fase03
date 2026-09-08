package br.com.postech.hospital.history.historico;

import br.com.postech.hospital.events.StatusConsulta;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ConsultaHistoricoResponseTest {

    @Test
    void deDeveMapearTodosOsCamposComDatasComoTextoIso() {
        UUID consultaId = UUID.randomUUID();
        UUID pacienteId = UUID.randomUUID();
        UUID medicoId = UUID.randomUUID();
        LocalDateTime dataHora = LocalDateTime.of(2026, 10, 1, 9, 0);
        LocalDateTime ocorridoEm = LocalDateTime.of(2026, 9, 30, 18, 0);
        ConsultaHistorico historico = new ConsultaHistorico(consultaId, pacienteId, medicoId, dataHora, StatusConsulta.AGENDADA, ocorridoEm);

        ConsultaHistoricoResponse response = ConsultaHistoricoResponse.de(historico);

        assertThat(response.id()).isEqualTo(consultaId);
        assertThat(response.pacienteId()).isEqualTo(pacienteId);
        assertThat(response.medicoId()).isEqualTo(medicoId);
        assertThat(response.dataHora()).isEqualTo(dataHora.toString());
        assertThat(response.status()).isEqualTo("AGENDADA");
        assertThat(response.atualizadoEm()).isEqualTo(ocorridoEm.toString());
    }
}
