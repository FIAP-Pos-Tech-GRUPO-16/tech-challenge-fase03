package br.com.postech.hospital.scheduling.messaging;

import br.com.postech.hospital.events.ConsultaEvent;
import br.com.postech.hospital.events.RabbitTopology;
import br.com.postech.hospital.events.TipoEventoConsulta;
import br.com.postech.hospital.scheduling.consulta.Consulta;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Publica no RabbitMQ o evento de integração correspondente a uma {@link ConsultaAlteradaEvent}.
 * Reage apenas depois que a transação que alterou a consulta é confirmada
 * ({@link TransactionPhase#AFTER_COMMIT}) — se a transação for revertida, nenhuma mensagem é
 * publicada, evitando notificar pacientes sobre uma consulta que não foi de fato persistida.
 *
 * <p>Esta abordagem reduz, mas não elimina, o problema clássico de "dual write" entre banco de
 * dados e broker: ainda existe uma janela entre o commit e a publicação em que o processo pode
 * cair. Para eliminar essa janela por completo seria necessário um Transactional Outbox (tabela
 * de eventos pendentes + um publicador assíncrono) — deliberadamente fora do escopo deste
 * desafio, mas é a evolução natural caso a garantia de entrega precise ser mais forte.
 */
@Component
public class ConsultaEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(ConsultaEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    public ConsultaEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void aoConfirmarAlteracaoDeConsulta(ConsultaAlteradaEvent evento) {
        Consulta consulta = evento.consulta();
        ConsultaEvent integrationEvent = evento.tipo() == TipoEventoConsulta.CRIADA
                ? ConsultaEvent.criada(consulta.getId(), consulta.getPacienteId(), consulta.getMedicoId(), consulta.getDataHora(), consulta.getStatus())
                : ConsultaEvent.editada(consulta.getId(), consulta.getPacienteId(), consulta.getMedicoId(), consulta.getDataHora(), consulta.getStatus());

        String routingKey = evento.tipo() == TipoEventoConsulta.CRIADA
                ? RabbitTopology.ROUTING_KEY_CONSULTA_CRIADA
                : RabbitTopology.ROUTING_KEY_CONSULTA_EDITADA;

        rabbitTemplate.convertAndSend(RabbitTopology.CONSULTAS_EXCHANGE, routingKey, integrationEvent);
        log.info("Evento publicado: tipo={} consultaId={} eventoId={}",
                integrationEvent.tipo(), integrationEvent.consultaId(), integrationEvent.eventoId());
    }
}
