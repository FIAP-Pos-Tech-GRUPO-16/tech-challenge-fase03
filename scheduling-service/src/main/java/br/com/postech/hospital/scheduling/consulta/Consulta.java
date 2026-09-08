package br.com.postech.hospital.scheduling.consulta;

import br.com.postech.hospital.events.StatusConsulta;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Consulta médica agendada por um médico ou enfermeiro para um paciente. É a entidade dona
 * do dado — os serviços de notificação e histórico nunca leem esta tabela diretamente, apenas
 * reagem aos eventos publicados quando ela é criada ou editada.
 */
@Entity
@Table(name = "consultas")
public class Consulta {

    @Id
    private UUID id;

    @Column(name = "paciente_id", nullable = false)
    private UUID pacienteId;

    @Column(name = "medico_id", nullable = false)
    private UUID medicoId;

    @Column(name = "registrada_por_id", nullable = false)
    private UUID registradaPorId;

    @Column(name = "data_hora", nullable = false)
    private LocalDateTime dataHora;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusConsulta status;

    @Column(length = 1000)
    private String observacoes;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm;

    protected Consulta() {
    }

    private Consulta(UUID id, UUID pacienteId, UUID medicoId, UUID registradaPorId,
                      LocalDateTime dataHora, StatusConsulta status, String observacoes) {
        this.id = id;
        this.pacienteId = Objects.requireNonNull(pacienteId, "pacienteId é obrigatório");
        this.medicoId = Objects.requireNonNull(medicoId, "medicoId é obrigatório");
        this.registradaPorId = Objects.requireNonNull(registradaPorId, "registradaPorId é obrigatório");
        this.dataHora = Objects.requireNonNull(dataHora, "dataHora é obrigatório");
        this.status = Objects.requireNonNull(status, "status é obrigatório");
        this.observacoes = observacoes;
        LocalDateTime agora = LocalDateTime.now();
        this.criadoEm = agora;
        this.atualizadoEm = agora;
    }

    public static Consulta agendar(UUID pacienteId, UUID medicoId, UUID registradaPorId,
                                    LocalDateTime dataHora, String observacoes) {
        return new Consulta(UUID.randomUUID(), pacienteId, medicoId, registradaPorId,
                dataHora, StatusConsulta.AGENDADA, observacoes);
    }

    public void atualizar(LocalDateTime novaDataHora, StatusConsulta novoStatus, String novasObservacoes) {
        this.dataHora = Objects.requireNonNull(novaDataHora, "dataHora é obrigatório");
        this.status = Objects.requireNonNull(novoStatus, "status é obrigatório");
        this.observacoes = novasObservacoes;
    }

    public boolean pertenceAoPaciente(UUID pacienteId) {
        return this.pacienteId.equals(pacienteId);
    }

    public UUID getId() {
        return id;
    }

    public UUID getPacienteId() {
        return pacienteId;
    }

    public UUID getMedicoId() {
        return medicoId;
    }

    public UUID getRegistradaPorId() {
        return registradaPorId;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }

    public StatusConsulta getStatus() {
        return status;
    }

    public String getObservacoes() {
        return observacoes;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }

    public LocalDateTime getAtualizadoEm() {
        return atualizadoEm;
    }

    @PrePersist
    void aoPersistir() {
        LocalDateTime agora = LocalDateTime.now();
        if (criadoEm == null) {
            criadoEm = agora;
        }
        atualizadoEm = agora;
    }

    @PreUpdate
    void aoAtualizar() {
        atualizadoEm = LocalDateTime.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Consulta consulta)) return false;
        return Objects.equals(id, consulta.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
