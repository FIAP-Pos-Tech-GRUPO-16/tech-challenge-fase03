package br.com.postech.hospital.scheduling.usuario;

import br.com.postech.hospital.security.SecurityRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioDetailsServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    private UsuarioDetailsService usuarioDetailsService;

    @Test
    void deveCarregarUsuarioComAutoridadeCorrespondenteAoPapel() {
        usuarioDetailsService = new UsuarioDetailsService(usuarioRepository);
        Usuario usuario = Usuario.novo("Dra. Ana Souza", "medica.ana", "hash-bcrypt", SecurityRole.MEDICO);
        when(usuarioRepository.findByUsername("medica.ana")).thenReturn(Optional.of(usuario));

        UserDetails userDetails = usuarioDetailsService.loadUserByUsername("medica.ana");

        assertThat(userDetails.getUsername()).isEqualTo("medica.ana");
        assertThat(userDetails.getPassword()).isEqualTo("hash-bcrypt");
        assertThat(userDetails.getAuthorities())
                .extracting(Object::toString)
                .containsExactly("ROLE_MEDICO");
    }

    @Test
    void deveLancarExcecaoQuandoUsuarioNaoExiste() {
        usuarioDetailsService = new UsuarioDetailsService(usuarioRepository);
        when(usuarioRepository.findByUsername("fantasma")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usuarioDetailsService.loadUserByUsername("fantasma"))
                .isInstanceOf(UsernameNotFoundException.class);
    }
}
