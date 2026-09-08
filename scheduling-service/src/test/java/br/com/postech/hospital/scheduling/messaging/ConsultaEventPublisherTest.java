package br.com.postech.hospital.scheduling.messaging;

import br.com.postech.hospital.events.ConsultaEvent;
import br.com.postech.hospital.events.RabbitTopology;
import br.com.postech.hospital.events.TipoEventoConsulta;
import br.com.postech.hospital.scheduling.consulta.Consulta;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ConsultaEventPublisherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    private final Consulta consulta = Consulta.agendar(
            UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), LocalDateTime.now().plusDays(1), null);

    @Test
    void devePublicarNaRoutingKeyDeCriacaoQuandoTipoCriada() {
        ConsultaEventPublisher publisher = new ConsultaEventPublisher(rabbitTemplate);

        publisher.aoConfirmarAlteracaoDeConsulta(new ConsultaAlteradaEvent(consulta, TipoEventoConsulta.CRIADA));

        ArgumentCaptor<ConsultaEvent> captor = ArgumentCaptor.forClass(ConsultaEvent.class);
        verify(rabbitTemplate).convertAndSend(
                org.mockito.ArgumentMatchers.eq(RabbitTopology.CONSULTAS_EXCHANGE),
                org.mockito.ArgumentMatchers.eq(RabbitTopology.ROUTING_KEY_CONSULTA_CRIADA),
                captor.capture());
        assertThat(captor.getValue().consultaId()).isEqualTo(consulta.getId());
        assertThat(captor.getValue().tipo()).isEqualTo(TipoEventoConsulta.CRIADA);
    }

    @Test
    void devePublicarNaRoutingKeyDeEdicaoQuandoTipoEditada() {
        ConsultaEventPublisher publisher = new ConsultaEventPublisher(rabbitTemplate);

        publisher.aoConfirmarAlteracaoDeConsulta(new ConsultaAlteradaEvent(consulta, TipoEventoConsulta.EDITADA));

        verify(rabbitTemplate).convertAndSend(
                org.mockito.ArgumentMatchers.eq(RabbitTopology.CONSULTAS_EXCHANGE),
                org.mockito.ArgumentMatchers.eq(RabbitTopology.ROUTING_KEY_CONSULTA_EDITADA),
                org.mockito.ArgumentMatchers.any(ConsultaEvent.class));
    }
}
