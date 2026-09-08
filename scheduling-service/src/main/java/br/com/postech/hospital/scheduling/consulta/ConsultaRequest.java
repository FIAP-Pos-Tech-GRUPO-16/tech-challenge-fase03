package br.com.postech.hospital.scheduling.consulta;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.UUID;

/** Dados para registrar uma nova consulta. Usado por médicos e enfermeiros. */
public record ConsultaRequest(
        @NotNull(message = "pacienteId é obrigatório") UUID pacienteId,
        @NotNull(message = "medicoId é obrigatório") UUID medicoId,
        @NotNull(message = "dataHora é obrigatório") @Future(message = "dataHora deve ser no futuro") LocalDateTime dataHora,
        @Size(max = 1000, message = "observacoes deve ter no máximo 1000 caracteres") String observacoes
) {
}
