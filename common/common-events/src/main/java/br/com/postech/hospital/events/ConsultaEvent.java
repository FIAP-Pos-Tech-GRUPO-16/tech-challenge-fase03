package br.com.postech.hospital.events;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Evento de integração publicado pelo serviço de agendamento sempre que uma consulta
 * é criada ou editada. É o único contrato entre o agendamento (produtor) e os serviços
 * de notificação e histórico (consumidores) — nenhum dos consumidores acessa o banco
 * de dados do agendamento diretamente.
 *
 * <p>{@code eventoId} é a chave de idempotência: cada consumidor deve descartar eventos
 * já processados com o mesmo id, já que o RabbitMQ garante entrega "at-least-once"
 * (o mesmo evento pode chegar mais de uma vez após uma falha e reentrega).
 *
 * <p>{@code ocorridoEm} é o instante em que o evento foi gerado na origem — não o instante
 * de recebimento — e permite que consumidores como o histórico ignorem eventos entregues
 * fora de ordem (ex.: uma edição antiga chegando depois de uma mais recente).
 */
public record ConsultaEvent(
        UUID eventoId,
        TipoEventoConsulta tipo,
        UUID consultaId,
        UUID pacienteId,
        UUID medicoId,
        LocalDateTime dataHora,
        StatusConsulta status,
        LocalDateTime ocorridoEm
) {

    public ConsultaEvent {
        Objects.requireNonNull(eventoId, "eventoId é obrigatório");
        Objects.requireNonNull(tipo, "tipo é obrigatório");
        Objects.requireNonNull(consultaId, "consultaId é obrigatório");
        Objects.requireNonNull(pacienteId, "pacienteId é obrigatório");
        Objects.requireNonNull(medicoId, "medicoId é obrigatório");
        Objects.requireNonNull(dataHora, "dataHora é obrigatório");
        Objects.requireNonNull(status, "status é obrigatório");
        Objects.requireNonNull(ocorridoEm, "ocorridoEm é obrigatório");
    }

    public static ConsultaEvent criada(UUID consultaId, UUID pacienteId, UUID medicoId,
                                        LocalDateTime dataHora, StatusConsulta status) {
        return new ConsultaEvent(UUID.randomUUID(), TipoEventoConsulta.CRIADA, consultaId,
                pacienteId, medicoId, dataHora, status, LocalDateTime.now());
    }

    public static ConsultaEvent editada(UUID consultaId, UUID pacienteId, UUID medicoId,
                                         LocalDateTime dataHora, StatusConsulta status) {
        return new ConsultaEvent(UUID.randomUUID(), TipoEventoConsulta.EDITADA, consultaId,
                pacienteId, medicoId, dataHora, status, LocalDateTime.now());
    }
}
