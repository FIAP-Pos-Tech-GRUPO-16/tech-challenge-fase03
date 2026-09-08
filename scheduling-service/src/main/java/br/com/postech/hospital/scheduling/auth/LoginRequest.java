package br.com.postech.hospital.scheduling.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @Schema(description = "Nome de usuário", example = "medica.ana")
        @NotBlank(message = "username é obrigatório") String username,

        @Schema(description = "Senha em texto puro (validada contra o hash armazenado)", example = "Senha@123")
        @NotBlank(message = "password é obrigatório") String password
) {
}
