package br.com.postech.hospital.scheduling.consulta;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.UUID;

/** Dados para registrar uma nova consulta. Usado por médicos e enfermeiros. */
public record ConsultaRequest(
        @Schema(description = "Id do usuário paciente (papel PACIENTE)")
        @NotNull(message = "pacienteId é obrigatório") UUID pacienteId,

        @Schema(description = "Id do usuário médico (papel MEDICO)")
        @NotNull(message = "medicoId é obrigatório") UUID medicoId,

        @Schema(description = "Data e hora da consulta, deve ser no futuro", example = "2027-01-15T10:00:00")
        @NotNull(message = "dataHora é obrigatório") @Future(message = "dataHora deve ser no futuro") LocalDateTime dataHora,

        @Schema(description = "Observações livres sobre a consulta (opcional)", example = "Consulta de rotina")
        @Size(max = 1000, message = "observacoes deve ter no máximo 1000 caracteres") String observacoes
) {
}
