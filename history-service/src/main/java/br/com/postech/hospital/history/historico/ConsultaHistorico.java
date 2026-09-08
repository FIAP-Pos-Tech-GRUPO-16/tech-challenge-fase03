package br.com.postech.hospital.history.historico;

import br.com.postech.hospital.events.StatusConsulta;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Modelo de leitura do histórico de consultas, reconstruído a partir dos eventos publicados
 * pelo agendamento — este serviço nunca acessa o banco de dados do agendamento diretamente.
 * A chave primária é o próprio {@code consultaId}, o que torna a atualização a partir de um
 * novo evento um upsert natural: {@link ConsultaHistoricoRepository#findById} seguido de save.
 */
@Entity
@Table(name = "consultas_historico")
public class ConsultaHistorico {

    @Id
    @Column(name = "consulta_id")
    private UUID consultaId;

    @Column(name = "paciente_id", nullable = false)
    private UUID pacienteId;

    @Column(name = "medico_id", nullable = false)
    private UUID medicoId;

    @Column(name = "data_hora", nullable = false)
    private LocalDateTime dataHora;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusConsulta status;

    @Column(name = "ultima_atualizacao_evento", nullable = false)
    private LocalDateTime ultimaAtualizacaoEvento;

    protected ConsultaHistorico() {
    }

    public ConsultaHistorico(UUID consultaId, UUID pacienteId, UUID medicoId, LocalDateTime dataHora,
                              StatusConsulta status, LocalDateTime ultimaAtualizacaoEvento) {
        this.consultaId = Objects.requireNonNull(consultaId, "consultaId é obrigatório");
        this.pacienteId = Objects.requireNonNull(pacienteId, "pacienteId é obrigatório");
        this.medicoId = Objects.requireNonNull(medicoId, "medicoId é obrigatório");
        this.dataHora = Objects.requireNonNull(dataHora, "dataHora é obrigatório");
        this.status = Objects.requireNonNull(status, "status é obrigatório");
        this.ultimaAtualizacaoEvento = Objects.requireNonNull(ultimaAtualizacaoEvento, "ultimaAtualizacaoEvento é obrigatório");
    }

    /**
     * Um evento só deve atualizar o registro se for mais recente que a última atualização já
     * aplicada — protege contra reentrega fora de ordem (ex.: uma edição antiga chegando depois
     * de uma mais nova, num cenário de retry após falha).
     */
    public boolean maisRecenteQue(LocalDateTime ocorridoEmDoEvento) {
        return ocorridoEmDoEvento.isAfter(this.ultimaAtualizacaoEvento);
    }

    public void aplicarEvento(UUID pacienteId, UUID medicoId, LocalDateTime dataHora,
                               StatusConsulta status, LocalDateTime ocorridoEmDoEvento) {
        this.pacienteId = pacienteId;
        this.medicoId = medicoId;
        this.dataHora = dataHora;
        this.status = status;
        this.ultimaAtualizacaoEvento = ocorridoEmDoEvento;
    }

    public UUID getConsultaId() {
        return consultaId;
    }

    public UUID getPacienteId() {
        return pacienteId;
    }

    public UUID getMedicoId() {
        return medicoId;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }

    public StatusConsulta getStatus() {
        return status;
    }

    public LocalDateTime getUltimaAtualizacaoEvento() {
        return ultimaAtualizacaoEvento;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ConsultaHistorico that)) return false;
        return Objects.equals(consultaId, that.consultaId);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(consultaId);
    }
}
