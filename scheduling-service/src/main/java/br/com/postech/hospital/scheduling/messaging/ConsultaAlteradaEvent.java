package br.com.postech.hospital.scheduling.messaging;

import br.com.postech.hospital.events.TipoEventoConsulta;
import br.com.postech.hospital.scheduling.consulta.Consulta;

/**
 * Evento interno do Spring (não confundir com {@link br.com.postech.hospital.events.ConsultaEvent},
 * que é o evento de integração publicado no RabbitMQ). Publicado pelo {@code ConsultaService}
 * ainda dentro da transação, e só disparado para o RabbitMQ depois que a transação der commit —
 * assim, uma falha ao salvar a consulta nunca resulta em uma notificação para um dado que não existe.
 */
public record ConsultaAlteradaEvent(Consulta consulta, TipoEventoConsulta tipo) {
}
