package br.com.postech.hospital.notification.notificacao;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Registro de um lembrete enviado a um paciente sobre uma consulta. {@code eventoId} é a chave
 * de idempotência: o mesmo evento de integração nunca gera duas notificações, mesmo que o
 * RabbitMQ o entregue mais de uma vez.
 */
@Entity
@Table(name = "notificacoes", uniqueConstraints = @UniqueConstraint(name = "uk_notificacoes_evento_id", columnNames = "evento_id"))
public class Notificacao {

    @Id
    private UUID id;

    @Column(name = "evento_id", nullable = false)
    private UUID eventoId;

    @Column(name = "consulta_id", nullable = false)
    private UUID consultaId;

    @Column(name = "paciente_id", nullable = false)
    private UUID pacienteId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CanalNotificacao canal;

    @Column(nullable = false, length = 500)
    private String mensagem;

    @Column(name = "enviada_em", nullable = false)
    private LocalDateTime enviadaEm;

    protected Notificacao() {
    }

    private Notificacao(UUID id, UUID eventoId, UUID consultaId, UUID pacienteId,
                         CanalNotificacao canal, String mensagem) {
        this.id = id;
        this.eventoId = Objects.requireNonNull(eventoId, "eventoId é obrigatório");
        this.consultaId = Objects.requireNonNull(consultaId, "consultaId é obrigatório");
        this.pacienteId = Objects.requireNonNull(pacienteId, "pacienteId é obrigatório");
        this.canal = Objects.requireNonNull(canal, "canal é obrigatório");
        this.mensagem = Objects.requireNonNull(mensagem, "mensagem é obrigatório");
        this.enviadaEm = LocalDateTime.now();
    }

    public static Notificacao paraLembreteDeConsulta(UUID eventoId, UUID consultaId, UUID pacienteId, String mensagem) {
        return new Notificacao(UUID.randomUUID(), eventoId, consultaId, pacienteId, CanalNotificacao.EMAIL, mensagem);
    }

    public UUID getId() {
        return id;
    }

    public UUID getEventoId() {
        return eventoId;
    }

    public UUID getConsultaId() {
        return consultaId;
    }

    public UUID getPacienteId() {
        return pacienteId;
    }

    public CanalNotificacao getCanal() {
        return canal;
    }

    public String getMensagem() {
        return mensagem;
    }

    public LocalDateTime getEnviadaEm() {
        return enviadaEm;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Notificacao that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
