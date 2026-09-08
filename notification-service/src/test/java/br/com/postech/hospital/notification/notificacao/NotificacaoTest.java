package br.com.postech.hospital.notification.notificacao;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NotificacaoTest {

    @Test
    void paraLembreteDeConsultaDeveCriarNotificacaoComCanalEmail() {
        UUID eventoId = UUID.randomUUID();
        UUID consultaId = UUID.randomUUID();
        UUID pacienteId = UUID.randomUUID();

        Notificacao notificacao = Notificacao.paraLembreteDeConsulta(eventoId, consultaId, pacienteId, "mensagem");

        assertThat(notificacao.getId()).isNotNull();
        assertThat(notificacao.getEventoId()).isEqualTo(eventoId);
        assertThat(notificacao.getConsultaId()).isEqualTo(consultaId);
        assertThat(notificacao.getPacienteId()).isEqualTo(pacienteId);
        assertThat(notificacao.getCanal()).isEqualTo(CanalNotificacao.EMAIL);
        assertThat(notificacao.getMensagem()).isEqualTo("mensagem");
        assertThat(notificacao.getEnviadaEm()).isNotNull();
    }

    @Test
    void deveRejeitarCamposObrigatoriosNulos() {
        UUID id = UUID.randomUUID();
        assertThatThrownBy(() -> Notificacao.paraLembreteDeConsulta(null, id, id, "m")).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Notificacao.paraLembreteDeConsulta(id, null, id, "m")).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Notificacao.paraLembreteDeConsulta(id, id, null, "m")).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Notificacao.paraLembreteDeConsulta(id, id, id, null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void duasNotificacoesComMesmoIdDevemSerIguais() {
        Notificacao notificacao = Notificacao.paraLembreteDeConsulta(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "m");

        assertThat(notificacao).isEqualTo(notificacao);
        assertThat(notificacao).isNotEqualTo(null);
    }
}
