package br.com.postech.hospital.scheduling.consulta;

import br.com.postech.hospital.events.StatusConsulta;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/** Dados para editar uma consulta existente. Usado por médicos e enfermeiros. */
public record ConsultaUpdateRequest(
        @Schema(description = "Nova data e hora da consulta", example = "2027-01-15T11:00:00")
        @NotNull(message = "dataHora é obrigatório") LocalDateTime dataHora,

        @Schema(description = "Novo status da consulta")
        @NotNull(message = "status é obrigatório") StatusConsulta status,

        @Schema(description = "Observações livres sobre a consulta (opcional)", example = "Reagendada 1h mais tarde")
        @Size(max = 1000, message = "observacoes deve ter no máximo 1000 caracteres") String observacoes
) {
}
