package br.com.postech.hospital.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthenticatedUserTest {

    @AfterEach
    void limparContexto() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void isPacienteDeveSerVerdadeiroSomenteParaPapelPaciente() {
        AuthenticatedUser paciente = new AuthenticatedUser(UUID.randomUUID(), "joao", SecurityRole.PACIENTE);
        AuthenticatedUser medico = new AuthenticatedUser(UUID.randomUUID(), "ana", SecurityRole.MEDICO);

        assertThat(paciente.isPaciente()).isTrue();
        assertThat(medico.isPaciente()).isFalse();
    }

    @Test
    void currentDeveRetornarUsuarioDoContextoDeSeguranca() {
        AuthenticatedUser usuario = new AuthenticatedUser(UUID.randomUUID(), "joao", SecurityRole.PACIENTE);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(usuario, null, List.of()));

        assertThat(AuthenticatedUser.current()).isEqualTo(usuario);
    }

    @Test
    void currentDeveLancarExcecaoQuandoNaoHaAutenticacao() {
        assertThatThrownBy(AuthenticatedUser::current)
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void construtorDeveRejeitarCamposObrigatoriosNulos() {
        assertThatThrownBy(() -> new AuthenticatedUser(null, "joao", SecurityRole.PACIENTE))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new AuthenticatedUser(UUID.randomUUID(), null, SecurityRole.PACIENTE))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new AuthenticatedUser(UUID.randomUUID(), "joao", null))
                .isInstanceOf(NullPointerException.class);
    }
}
