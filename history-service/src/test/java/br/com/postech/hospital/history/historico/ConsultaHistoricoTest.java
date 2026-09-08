package br.com.postech.hospital.history.historico;

import br.com.postech.hospital.events.StatusConsulta;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConsultaHistoricoTest {

    private final UUID consultaId = UUID.randomUUID();
    private final UUID pacienteId = UUID.randomUUID();
    private final UUID medicoId = UUID.randomUUID();
    private final LocalDateTime dataHora = LocalDateTime.now().plusDays(1);
    private final LocalDateTime ocorridoEm = LocalDateTime.now();

    @Test
    void construtorDeveInicializarTodosOsCampos() {
        ConsultaHistorico historico = new ConsultaHistorico(consultaId, pacienteId, medicoId, dataHora, StatusConsulta.AGENDADA, ocorridoEm);

        assertThat(historico.getConsultaId()).isEqualTo(consultaId);
        assertThat(historico.getPacienteId()).isEqualTo(pacienteId);
        assertThat(historico.getMedicoId()).isEqualTo(medicoId);
        assertThat(historico.getDataHora()).isEqualTo(dataHora);
        assertThat(historico.getStatus()).isEqualTo(StatusConsulta.AGENDADA);
        assertThat(historico.getUltimaAtualizacaoEvento()).isEqualTo(ocorridoEm);
    }

    @Test
    void maisRecenteQueDeveCompararComAUltimaAtualizacao() {
        ConsultaHistorico historico = new ConsultaHistorico(consultaId, pacienteId, medicoId, dataHora, StatusConsulta.AGENDADA, ocorridoEm);

        assertThat(historico.maisRecenteQue(ocorridoEm.plusSeconds(1))).isTrue();
        assertThat(historico.maisRecenteQue(ocorridoEm.minusSeconds(1))).isFalse();
        assertThat(historico.maisRecenteQue(ocorridoEm)).isFalse();
    }

    @Test
    void aplicarEventoDeveSubstituirCamposEUltimaAtualizacao() {
        ConsultaHistorico historico = new ConsultaHistorico(consultaId, pacienteId, medicoId, dataHora, StatusConsulta.AGENDADA, ocorridoEm);
        LocalDateTime novoOcorridoEm = ocorridoEm.plusMinutes(5);
        LocalDateTime novaDataHora = dataHora.plusHours(2);

        historico.aplicarEvento(pacienteId, medicoId, novaDataHora, StatusConsulta.REALIZADA, novoOcorridoEm);

        assertThat(historico.getDataHora()).isEqualTo(novaDataHora);
        assertThat(historico.getStatus()).isEqualTo(StatusConsulta.REALIZADA);
        assertThat(historico.getUltimaAtualizacaoEvento()).isEqualTo(novoOcorridoEm);
    }

    @Test
    void deveRejeitarCamposObrigatoriosNulos() {
        assertThatThrownBy(() -> new ConsultaHistorico(null, pacienteId, medicoId, dataHora, StatusConsulta.AGENDADA, ocorridoEm))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new ConsultaHistorico(consultaId, pacienteId, medicoId, dataHora, null, ocorridoEm))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void duasInstanciasComMesmoConsultaIdDevemSerIguais() {
        ConsultaHistorico historico = new ConsultaHistorico(consultaId, pacienteId, medicoId, dataHora, StatusConsulta.AGENDADA, ocorridoEm);

        assertThat(historico).isEqualTo(historico);
        assertThat(historico).isNotEqualTo(null);
    }
}
