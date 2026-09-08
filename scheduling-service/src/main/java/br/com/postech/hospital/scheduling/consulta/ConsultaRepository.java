package br.com.postech.hospital.scheduling.consulta;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ConsultaRepository extends JpaRepository<Consulta, UUID> {

    List<Consulta> findByPacienteId(UUID pacienteId);
}
