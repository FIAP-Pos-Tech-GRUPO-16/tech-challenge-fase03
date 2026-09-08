package br.com.postech.hospital.scheduling.consulta;

import br.com.postech.hospital.events.StatusConsulta;

import java.time.LocalDateTime;
import java.util.UUID;

public record ConsultaResponse(
        UUID id,
        UUID pacienteId,
        UUID medicoId,
        UUID registradaPorId,
        LocalDateTime dataHora,
        StatusConsulta status,
        String observacoes,
        LocalDateTime criadoEm,
        LocalDateTime atualizadoEm
) {

    public static ConsultaResponse de(Consulta consulta) {
        return new ConsultaResponse(
                consulta.getId(),
                consulta.getPacienteId(),
                consulta.getMedicoId(),
                consulta.getRegistradaPorId(),
                consulta.getDataHora(),
                consulta.getStatus(),
                consulta.getObservacoes(),
                consulta.getCriadoEm(),
                consulta.getAtualizadoEm()
        );
    }
}
