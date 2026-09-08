package br.com.postech.hospital.scheduling.config;

import br.com.postech.hospital.events.RabbitTopology;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * O agendamento é o dono da exchange de consultas — os consumidores (notificação, histórico)
 * declaram suas próprias filas e bindings apontando para ela.
 */
@Configuration
public class RabbitMqConfig {

    @Bean
    public TopicExchange consultasExchange() {
        return new TopicExchange(RabbitTopology.CONSULTAS_EXCHANGE, true, false);
    }

    @Bean
    public MessageConverter messageConverter() {
        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return new Jackson2JsonMessageConverter(mapper);
    }
}
