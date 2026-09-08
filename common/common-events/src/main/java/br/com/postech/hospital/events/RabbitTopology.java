package br.com.postech.hospital.events;

/**
 * Nomes de exchange, routing keys, filas e DLQs usados na comunicação assíncrona entre
 * o agendamento (produtor) e os consumidores (notificação, histórico).
 *
 * <p>Centralizar esses nomes aqui evita divergência de configuração entre os três serviços,
 * que são deployados e evoluídos de forma independente mas precisam concordar sobre a
 * topologia do broker.
 */
public final class RabbitTopology {

    public static final String CONSULTAS_EXCHANGE = "consultas.exchange";
    public static final String CONSULTAS_DLX = "consultas.dlx";

    public static final String ROUTING_KEY_CONSULTA_CRIADA = "consulta.criada";
    public static final String ROUTING_KEY_CONSULTA_EDITADA = "consulta.editada";
    public static final String ROUTING_KEY_CONSULTA_TODAS = "consulta.*";

    public static final String QUEUE_NOTIFICACAO = "notificacao.consultas.queue";
    public static final String QUEUE_NOTIFICACAO_DLQ = "notificacao.consultas.queue.dlq";

    public static final String QUEUE_HISTORICO = "historico.consultas.queue";
    public static final String QUEUE_HISTORICO_DLQ = "historico.consultas.queue.dlq";

    private RabbitTopology() {
    }
}
