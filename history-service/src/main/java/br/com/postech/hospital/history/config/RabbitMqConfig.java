package br.com.postech.hospital.history.config;

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
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.retry.RejectAndDontRequeueRecoverer;
import org.springframework.amqp.support.converter.DefaultJackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.interceptor.RetryOperationsInterceptor;

/** Mesma topologia de retry/DLQ do serviço de notificação, aplicada à fila de histórico. */
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
    public Queue historicoQueue() {
        return QueueBuilder.durable(RabbitTopology.QUEUE_HISTORICO)
                .withArgument("x-dead-letter-exchange", RabbitTopology.CONSULTAS_DLX)
                .withArgument("x-dead-letter-routing-key", RabbitTopology.QUEUE_HISTORICO_DLQ)
                .build();
    }

    @Bean
    public Queue historicoDlq() {
        return QueueBuilder.durable(RabbitTopology.QUEUE_HISTORICO_DLQ).build();
    }

    @Bean
    public Binding historicoBinding() {
        return BindingBuilder.bind(historicoQueue())
                .to(consultasExchange())
                .with(RabbitTopology.ROUTING_KEY_CONSULTA_TODAS);
    }

    @Bean
    public Binding historicoDlqBinding() {
        return BindingBuilder.bind(historicoDlq())
                .to(consultasDlx())
                .with(RabbitTopology.QUEUE_HISTORICO_DLQ);
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
