package br.com.postech.hospital.history.messaging;

import br.com.postech.hospital.events.ConsultaEvent;
import br.com.postech.hospital.events.RabbitTopology;
import br.com.postech.hospital.history.historico.HistoricoService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class ConsultaEventListener {

    private final HistoricoService historicoService;

    public ConsultaEventListener(HistoricoService historicoService) {
        this.historicoService = historicoService;
    }

    @RabbitListener(queues = RabbitTopology.QUEUE_HISTORICO)
    public void aoReceberEventoDeConsulta(ConsultaEvent evento) {
        historicoService.aplicar(evento);
    }
}
