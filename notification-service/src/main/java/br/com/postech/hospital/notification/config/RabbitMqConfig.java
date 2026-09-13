package br.com.postech.hospital.notification.config;

import br.com.postech.hospital.events.ConsultaEvent;
import br.com.postech.hospital.events.RabbitTopology;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.retry.RejectAndDontRequeueRecoverer;
import org.springframework.amqp.support.converter.DefaultJackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.interceptor.RetryOperationsInterceptor;

/**
 * Declara a fila de notificação e sua dead-letter queue, e configura o listener para tentar
 * reprocessar uma mensagem até 3 vezes (com backoff) antes de desistir e mandá-la para a DLQ —
 * evitando que uma falha transitória (ex.: banco fora do ar por um instante) descarte um
 * lembrete, sem deixar uma mensagem permanentemente "presa" travando a fila em caso de erro
 * persistente (ex.: payload inválido).
 */
@Configuration
public class RabbitMqConfig {

    @Bean
    public TopicExchange consultasExchange() {
        return new TopicExchange(RabbitTopology.CONSULTAS_EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange consultasDlx() {
        return new DirectExchange(RabbitTopology.CONSULTAS_DLX, true, false);
    }

    @Bean
    public Queue notificacaoQueue() {
        return QueueBuilder.durable(RabbitTopology.QUEUE_NOTIFICACAO)
                .withArgument("x-dead-letter-exchange", RabbitTopology.CONSULTAS_DLX)
                .withArgument("x-dead-letter-routing-key", RabbitTopology.QUEUE_NOTIFICACAO_DLQ)
                .build();
    }

    @Bean
    public Queue notificacaoDlq() {
        return QueueBuilder.durable(RabbitTopology.QUEUE_NOTIFICACAO_DLQ).build();
    }

    @Bean
    public Binding notificacaoBinding() {
        return BindingBuilder.bind(notificacaoQueue())
                .to(consultasExchange())
                .with(RabbitTopology.ROUTING_KEY_CONSULTA_TODAS);
    }

    @Bean
    public Binding notificacaoDlqBinding() {
        return BindingBuilder.bind(notificacaoDlq())
                .to(consultasDlx())
                .with(RabbitTopology.QUEUE_NOTIFICACAO_DLQ);
    }

    @Bean
    public MessageConverter messageConverter() {
        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        DefaultJackson2JavaTypeMapper typeMapper = new DefaultJackson2JavaTypeMapper();
        typeMapper.setTrustedPackages(ConsultaEvent.class.getPackageName());

        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter(mapper);
        converter.setJavaTypeMapper(typeMapper);
        return converter;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter);
        factory.setAdviceChain(retryInterceptor());
        return factory;
    }

    private RetryOperationsInterceptor retryInterceptor() {
        return RetryInterceptorBuilder.stateless()
                .maxAttempts(3)
                .backOffOptions(1000, 2.0, 10000)
                .recoverer(new RejectAndDontRequeueRecoverer())
                .build();
    }
}
