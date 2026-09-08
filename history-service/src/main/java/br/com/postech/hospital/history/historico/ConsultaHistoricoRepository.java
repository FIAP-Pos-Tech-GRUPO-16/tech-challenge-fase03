package br.com.postech.hospital.history.historico;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface ConsultaHistoricoRepository extends JpaRepository<ConsultaHistorico, UUID> {

    List<ConsultaHistorico> findByPacienteId(UUID pacienteId);

    List<ConsultaHistorico> findByPacienteIdAndDataHoraAfter(UUID pacienteId, LocalDateTime referencia);
}
