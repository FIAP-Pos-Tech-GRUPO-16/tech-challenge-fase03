package br.com.postech.hospital.notification.notificacao;

import java.time.LocalDateTime;
import java.util.UUID;

public record NotificacaoResponse(
        UUID id,
        UUID consultaId,
        UUID pacienteId,
        CanalNotificacao canal,
        String mensagem,
        LocalDateTime enviadaEm
) {

    public static NotificacaoResponse de(Notificacao notificacao) {
        return new NotificacaoResponse(
                notificacao.getId(),
                notificacao.getConsultaId(),
                notificacao.getPacienteId(),
                notificacao.getCanal(),
                notificacao.getMensagem(),
                notificacao.getEnviadaEm()
        );
    }
}
