package br.com.postech.hospital.notification.messaging;

import br.com.postech.hospital.events.ConsultaEvent;
import br.com.postech.hospital.events.StatusConsulta;
import br.com.postech.hospital.events.TipoEventoConsulta;
import br.com.postech.hospital.notification.config.RabbitMqConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.MessageConverter;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Contrato de serializacao do evento de consulta")
class ConsultaEventContratoTest {

    private final MessageConverter converter = new RabbitMqConfig().messageConverter();

    private ConsultaEvent consultaCriada() {
        return new ConsultaEvent(
                UUID.randomUUID(),
                TipoEventoConsulta.CRIADA,
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                LocalDateTime.of(2030, 1, 15, 10, 30, 0),
                StatusConsulta.AGENDADA,
                LocalDateTime.of(2026, 9, 13, 8, 0, 0));
    }

    @Test
    @DisplayName("evento sobrevive ao round-trip JSON do RabbitMQ sem perder nenhum campo")
    void eventoDeveSobreviverAoRoundTripDoConversor() {
        ConsultaEvent original = consultaCriada();

        Message mensagem = converter.toMessage(original, new MessageProperties());
        Object reconstruido = converter.fromMessage(mensagem);

        assertThat(reconstruido)
                .isInstanceOf(ConsultaEvent.class)
                .isEqualTo(original);
    }

    @Test
    @DisplayName("datas viajam como texto ISO-8601, nao como timestamp numerico")
    void datasDevemSerSerializadasComoTextoIso() {
        Message mensagem = converter.toMessage(consultaCriada(), new MessageProperties());
        String json = new String(mensagem.getBody());

        assertThat(json).contains("2030-01-15T10:30:00");
        assertThat(mensagem.getMessageProperties().getContentType())
                .isEqualTo(MessageProperties.CONTENT_TYPE_JSON);
    }

    @Test
    @DisplayName("o tipo concreto viaja na mensagem, permitindo ao consumidor reconstruir o evento")
    void mensagemDeveCarregarOTipoDoEvento() {
        Message mensagem = converter.toMessage(consultaCriada(), new MessageProperties());

        assertThat(mensagem.getMessageProperties().getHeaders())
                .containsEntry("__TypeId__", ConsultaEvent.class.getName());
    }

    @Test
    @DisplayName("mensagem apontando para classe fora do contrato é recusada")
    void classeForaDoPacoteDoContratoDeveSerRecusada() {
        MessageProperties propriedades = new MessageProperties();
        propriedades.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        propriedades.getHeaders().put("__TypeId__", "java.io.File");

        Message forjada = new Message("{}".getBytes(StandardCharsets.UTF_8), propriedades);

        assertThatThrownBy(() -> converter.fromMessage(forjada))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("trusted packages");
    }
}
