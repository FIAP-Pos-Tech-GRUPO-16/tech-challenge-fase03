package br.com.postech.hospital.scheduling.auth;

import br.com.postech.hospital.scheduling.usuario.Usuario;
import br.com.postech.hospital.scheduling.usuario.UsuarioRepository;
import br.com.postech.hospital.security.AuthenticatedUser;
import br.com.postech.hospital.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

/**
 * O agendamento é a única autoridade de autenticação do sistema: valida usuário/senha e emite
 * o JWT que notificação e histórico aceitam sem precisar chamá-lo de volta.
 */
@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UsuarioRepository usuarioRepository;
    private final JwtService jwtService;

    public AuthService(AuthenticationManager authenticationManager, UsuarioRepository usuarioRepository, JwtService jwtService) {
        this.authenticationManager = authenticationManager;
        this.usuarioRepository = usuarioRepository;
        this.jwtService = jwtService;
    }

    public LoginResponse autenticar(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password()));

        Usuario usuario = usuarioRepository.findByUsername(request.username())
                .orElseThrow(() -> new IllegalStateException("Usuário autenticado não encontrado: " + request.username()));

        AuthenticatedUser authenticatedUser = new AuthenticatedUser(usuario.getId(), usuario.getUsername(), usuario.getRole());
        String token = jwtService.gerarToken(authenticatedUser);

        return new LoginResponse(token, "Bearer", usuario.getRole(), jwtService.expiracao().toMinutes());
    }
}
