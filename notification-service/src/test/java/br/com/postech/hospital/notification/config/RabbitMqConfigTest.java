package br.com.postech.hospital.notification.config;

import br.com.postech.hospital.events.ConsultaEvent;
import br.com.postech.hospital.events.RabbitTopology;
import br.com.postech.hospital.events.StatusConsulta;
import br.com.postech.hospital.events.TipoEventoConsulta;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.MessageConverter;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

@DisplayName("Topologia RabbitMQ do serviço de notificações")
class RabbitMqConfigTest {

    private final RabbitMqConfig config = new RabbitMqConfig();

    @Test
    @DisplayName("exchange principal é topic e durável")
    void exchangePrincipalDeveSerTopicDuravel() {
        TopicExchange exchange = config.consultasExchange();

        assertThat(exchange.getName()).isEqualTo(RabbitTopology.CONSULTAS_EXCHANGE);
        assertThat(exchange.isDurable()).isTrue();
        assertThat(exchange.isAutoDelete()).isFalse();
    }

    @Test
    @DisplayName("exchange de erro é direct e durável")
    void dlxDeveSerDirectDuravel() {
        DirectExchange dlx = config.consultasDlx();

        assertThat(dlx.getName()).isEqualTo(RabbitTopology.CONSULTAS_DLX);
        assertThat(dlx.isDurable()).isTrue();
    }

    @Test
    @DisplayName("fila encaminha falhas para a DLX com a chave da própria DLQ")
    void filaDeveEncaminharFalhasParaDlq() {
        Queue fila = config.notificacaoQueue();

        assertThat(fila.getName()).isEqualTo(RabbitTopology.QUEUE_NOTIFICACAO);
        assertThat(fila.isDurable()).isTrue();
        assertThat(fila.getArguments())
                .containsEntry("x-dead-letter-exchange", RabbitTopology.CONSULTAS_DLX)
                .containsEntry("x-dead-letter-routing-key", RabbitTopology.QUEUE_NOTIFICACAO_DLQ);
    }

    @Test
    @DisplayName("DLQ é durável, para não perder mensagem com falha permanente")
    void dlqDeveSerDuravel() {
        Queue dlq = config.notificacaoDlq();

        assertThat(dlq.getName()).isEqualTo(RabbitTopology.QUEUE_NOTIFICACAO_DLQ);
        assertThat(dlq.isDurable()).isTrue();
    }

    @Test
    @DisplayName("binding escuta consulta.* — pega criada e editada")
    void bindingDeveEscutarTodosOsEventosDeConsulta() {
        Binding binding = config.notificacaoBinding();

        assertThat(binding.getExchange()).isEqualTo(RabbitTopology.CONSULTAS_EXCHANGE);
        assertThat(binding.getDestination()).isEqualTo(RabbitTopology.QUEUE_NOTIFICACAO);
        assertThat(binding.getRoutingKey()).isEqualTo(RabbitTopology.ROUTING_KEY_CONSULTA_TODAS);
    }

    @Test
    @DisplayName("as routing keys publicadas casam com o padrão do binding")
    void routingKeysPublicadasDevemCasarComOBinding() {
        String padrao = RabbitTopology.ROUTING_KEY_CONSULTA_TODAS.replace("*", "[^.]+");

        assertThat(RabbitTopology.ROUTING_KEY_CONSULTA_CRIADA).matches(padrao);
        assertThat(RabbitTopology.ROUTING_KEY_CONSULTA_EDITADA).matches(padrao);
    }

    @Test
    @DisplayName("binding da DLQ liga a fila de erro à DLX")
    void bindingDaDlqDeveLigarNaDlx() {
        Binding binding = config.notificacaoDlqBinding();

        assertThat(binding.getExchange()).isEqualTo(RabbitTopology.CONSULTAS_DLX);
        assertThat(binding.getDestination()).isEqualTo(RabbitTopology.QUEUE_NOTIFICACAO_DLQ);
        assertThat(binding.getRoutingKey()).isEqualTo(RabbitTopology.QUEUE_NOTIFICACAO_DLQ);
    }

    @Test
    @DisplayName("evento sobrevive ao round-trip JSON, inclusive as datas")
    void eventoDeveSobreviverAoRoundTripDoConversor() {
        MessageConverter converter = config.messageConverter();
        ConsultaEvent original = new ConsultaEvent(UUID.randomUUID(), TipoEventoConsulta.CRIADA,
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                LocalDateTime.of(2030, 1, 15, 10, 30), StatusConsulta.AGENDADA,
                LocalDateTime.of(2026, 9, 13, 8, 0));

        Message mensagem = converter.toMessage(original, new MessageProperties());

        assertThat(new String(mensagem.getBody(), StandardCharsets.UTF_8)).contains("2030-01-15T10:30");
        assertThat(converter.fromMessage(mensagem)).isEqualTo(original);
    }

    @Test
    @DisplayName("classe fora do contrato é recusada — não confiamos em \"*\"")
    void classeForaDoPacoteDoContratoDeveSerRecusada() {
        MessageConverter converter = config.messageConverter();
        MessageProperties propriedades = new MessageProperties();
        propriedades.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        propriedades.getHeaders().put("__TypeId__", "java.io.File");
        Message forjada = new Message("{}".getBytes(StandardCharsets.UTF_8), propriedades);

        assertThatThrownBy(() -> converter.fromMessage(forjada))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("trusted packages");
    }

    @Test
    @DisplayName("container de listener é criado com o conversor do projeto")
    void containerFactoryDeveSerCriado() {
        ConnectionFactory connectionFactory = mock(ConnectionFactory.class);

        SimpleRabbitListenerContainerFactory factory =
                config.rabbitListenerContainerFactory(connectionFactory, config.messageConverter());

        assertThat(factory).isNotNull();
    }
}
