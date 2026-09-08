package br.com.postech.hospital.scheduling.consulta;

import br.com.postech.hospital.events.StatusConsulta;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/** Dados para editar uma consulta existente. Usado por médicos e enfermeiros. */
public record ConsultaUpdateRequest(
        @NotNull(message = "dataHora é obrigatório") LocalDateTime dataHora,
        @NotNull(message = "status é obrigatório") StatusConsulta status,
        @Size(max = 1000, message = "observacoes deve ter no máximo 1000 caracteres") String observacoes
) {
}
