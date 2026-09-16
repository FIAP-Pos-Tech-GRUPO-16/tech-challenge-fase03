package br.com.postech.hospital.scheduling.consulta;

import br.com.postech.hospital.events.StatusConsulta;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConsultaTest {

    private final UUID pacienteId = UUID.randomUUID();
    private final UUID medicoId = UUID.randomUUID();
    private final UUID registradaPorId = UUID.randomUUID();
    private final LocalDateTime dataHora = LocalDateTime.now().plusDays(2);

    @Test
    void agendarDeveCriarConsultaComStatusAgendada() {
        Consulta consulta = Consulta.agendar(pacienteId, medicoId, registradaPorId, dataHora, "primeira consulta");

        assertThat(consulta.getId()).isNotNull();
        assertThat(consulta.getPacienteId()).isEqualTo(pacienteId);
        assertThat(consulta.getMedicoId()).isEqualTo(medicoId);
        assertThat(consulta.getRegistradaPorId()).isEqualTo(registradaPorId);
        assertThat(consulta.getDataHora()).isEqualTo(dataHora);
        assertThat(consulta.getStatus()).isEqualTo(StatusConsulta.AGENDADA);
        assertThat(consulta.getObservacoes()).isEqualTo("primeira consulta");
    }

    @Test
    void atualizarDeveAlterarDataHoraStatusEObservacoes() {
        Consulta consulta = Consulta.agendar(pacienteId, medicoId, registradaPorId, dataHora, "obs original");
        LocalDateTime novaData = dataHora.plusDays(1);

        consulta.atualizar(novaData, StatusConsulta.REALIZADA, "obs atualizada");

        assertThat(consulta.getDataHora()).isEqualTo(novaData);
        assertThat(consulta.getStatus()).isEqualTo(StatusConsulta.REALIZADA);
        assertThat(consulta.getObservacoes()).isEqualTo("obs atualizada");
    }

    @Test
    void pertenceAoPacienteDeveCompararComOIdInformado() {
        Consulta consulta = Consulta.agendar(pacienteId, medicoId, registradaPorId, dataHora, null);

        assertThat(consulta.pertenceAoPaciente(pacienteId)).isTrue();
        assertThat(consulta.pertenceAoPaciente(UUID.randomUUID())).isFalse();
    }

    @Test
    void agendarDeveRejeitarCamposObrigatoriosNulos() {
        assertThatThrownBy(() -> Consulta.agendar(null, medicoId, registradaPorId, dataHora, null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Consulta.agendar(pacienteId, null, registradaPorId, dataHora, null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Consulta.agendar(pacienteId, medicoId, registradaPorId, null, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void duasConsultasComMesmoIdDevemSerIguais() {
        Consulta consulta = Consulta.agendar(pacienteId, medicoId, registradaPorId, dataHora, null);

        assertThat(consulta).isEqualTo(consulta);
        assertThat(consulta).isNotEqualTo(Consulta.agendar(pacienteId, medicoId, registradaPorId, dataHora, null));
        assertThat(consulta).isNotEqualTo(null);
        assertThat(consulta).isNotEqualTo("outro tipo");
    }

    @Test
    void consultasComIdsDiferentesNaoDevemTerOMesmoHashCode() {
        Consulta a = Consulta.agendar(pacienteId, medicoId, registradaPorId, dataHora, null);
        Consulta b = Consulta.agendar(pacienteId, medicoId, registradaPorId, dataHora, null);

        assertThat(a.hashCode()).isNotEqualTo(b.hashCode());
    }

    @Test
    void aoPersistirDeveDefinirCriadoEmQuandoEntidadeAindaNaoTemData() {
        // Simula a instância "vazia" que o Hibernate materializa via o construtor de pacote
        // antes de popular os campos — cenário em que criadoEm ainda não foi definido.
        Consulta consulta = new Consulta();

        consulta.aoPersistir();

        assertThat(consulta.getCriadoEm()).isNotNull();
        assertThat(consulta.getAtualizadoEm()).isEqualTo(consulta.getCriadoEm());
    }

    @Test
    void aoPersistirNaoDeveSobrescreverCriadoEmJaDefinido() {
        Consulta consulta = Consulta.agendar(pacienteId, medicoId, registradaPorId, dataHora, null);
        LocalDateTime criadoEmOriginal = consulta.getCriadoEm();

        consulta.aoPersistir();

        assertThat(consulta.getCriadoEm()).isEqualTo(criadoEmOriginal);
    }

    @Test
    void aoAtualizarDeveRenovarAtualizadoEm() throws InterruptedException {
        Consulta consulta = Consulta.agendar(pacienteId, medicoId, registradaPorId, dataHora, null);
        LocalDateTime atualizadoEmOriginal = consulta.getAtualizadoEm();
        Thread.sleep(1);

        consulta.aoAtualizar();

        assertThat(consulta.getAtualizadoEm()).isAfter(atualizadoEmOriginal);
    }
}
