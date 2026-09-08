package br.com.postech.hospital.events;

/**
 * Status possíveis de uma consulta médica ao longo do seu ciclo de vida.
 * Compartilhado entre o serviço de agendamento (dono do dado) e os consumidores
 * de eventos (notificação e histórico), que reconstroem esse estado a partir dos eventos.
 */
public enum StatusConsulta {
    AGENDADA,
    REALIZADA,
    CANCELADA
}
