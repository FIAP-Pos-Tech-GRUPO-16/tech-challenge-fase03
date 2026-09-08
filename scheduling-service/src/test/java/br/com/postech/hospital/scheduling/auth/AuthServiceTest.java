package br.com.postech.hospital.scheduling.auth;

import br.com.postech.hospital.scheduling.usuario.Usuario;
import br.com.postech.hospital.scheduling.usuario.UsuarioRepository;
import br.com.postech.hospital.security.AuthenticatedUser;
import br.com.postech.hospital.security.JwtService;
import br.com.postech.hospital.security.SecurityRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private JwtService jwtService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(authenticationManager, usuarioRepository, jwtService);
    }

    @Test
    void autenticarComSucessoDeveRetornarTokenEPapelDoUsuario() {
        LoginRequest request = new LoginRequest("medica.ana", "Senha@123");
        Usuario usuario = Usuario.novo("Dra. Ana Souza", "medica.ana", "hash", SecurityRole.MEDICO);
        when(usuarioRepository.findByUsername("medica.ana")).thenReturn(Optional.of(usuario));
        when(jwtService.gerarToken(any(AuthenticatedUser.class))).thenReturn("token-jwt");
        when(jwtService.expiracao()).thenReturn(Duration.ofMinutes(120));

        LoginResponse response = authService.autenticar(request);

        assertThat(response.token()).isEqualTo("token-jwt");
        assertThat(response.tipo()).isEqualTo("Bearer");
        assertThat(response.papel()).isEqualTo(SecurityRole.MEDICO);
        assertThat(response.expiraEmMinutos()).isEqualTo(120);

        ArgumentCaptor<UsernamePasswordAuthenticationToken> captor = ArgumentCaptor.forClass(UsernamePasswordAuthenticationToken.class);
        org.mockito.Mockito.verify(authenticationManager).authenticate(captor.capture());
        assertThat(captor.getValue().getPrincipal()).isEqualTo("medica.ana");
        assertThat(captor.getValue().getCredentials()).isEqualTo("Senha@123");
    }

    @Test
    void autenticarDevePropagarCredenciaisInvalidas() {
        LoginRequest request = new LoginRequest("medica.ana", "senha-errada");
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("credenciais inválidas"));

        assertThatThrownBy(() -> authService.autenticar(request))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void autenticarDeveLancarEstadoIlegalQuandoUsuarioNaoEncontradoAposAutenticar() {
        LoginRequest request = new LoginRequest("fantasma", "Senha@123");
        when(usuarioRepository.findByUsername("fantasma")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.autenticar(request))
                .isInstanceOf(IllegalStateException.class);
    }
}
