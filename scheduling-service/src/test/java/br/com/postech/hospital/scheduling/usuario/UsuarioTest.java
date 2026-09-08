package br.com.postech.hospital.scheduling.usuario;

import br.com.postech.hospital.security.SecurityRole;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UsuarioTest {

    @Test
    void novoDeveCriarUsuarioComIdGerado() {
        Usuario usuario = Usuario.novo("Dra. Ana Souza", "medica.ana", "hash", SecurityRole.MEDICO);

        assertThat(usuario.getId()).isNotNull();
        assertThat(usuario.getNomeCompleto()).isEqualTo("Dra. Ana Souza");
        assertThat(usuario.getUsername()).isEqualTo("medica.ana");
        assertThat(usuario.getPasswordHash()).isEqualTo("hash");
        assertThat(usuario.getRole()).isEqualTo(SecurityRole.MEDICO);
        assertThat(usuario.getCriadoEm()).isNotNull();
    }

    @Test
    void deveRejeitarCamposObrigatoriosNulos() {
        assertThatThrownBy(() -> Usuario.novo(null, "u", "h", SecurityRole.MEDICO)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Usuario.novo("n", null, "h", SecurityRole.MEDICO)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Usuario.novo("n", "u", null, SecurityRole.MEDICO)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Usuario.novo("n", "u", "h", null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void doisUsuariosComMesmoIdDevemSerIguais() {
        Usuario usuario = Usuario.novo("n", "u", "h", SecurityRole.PACIENTE);

        assertThat(usuario).isEqualTo(usuario);
        assertThat(usuario).isNotEqualTo(Usuario.novo("n", "u2", "h", SecurityRole.PACIENTE));
        assertThat(usuario).isNotEqualTo(null);
    }
}
