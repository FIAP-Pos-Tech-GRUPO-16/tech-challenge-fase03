package br.com.postech.hospital.notification.messaging;

import br.com.postech.hospital.events.ConsultaEvent;
import br.com.postech.hospital.events.RabbitTopology;
import br.com.postech.hospital.notification.notificacao.NotificacaoService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class ConsultaEventListener {

    private final NotificacaoService notificacaoService;

    public ConsultaEventListener(NotificacaoService notificacaoService) {
        this.notificacaoService = notificacaoService;
    }

    @RabbitListener(queues = RabbitTopology.QUEUE_NOTIFICACAO)
    public void aoReceberEventoDeConsulta(ConsultaEvent evento) {
        notificacaoService.processar(evento);
    }
}
