package br.com.postech.hospital.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private static final String SECRET = "01234567890123456789012345678901"; // 33 chars

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(new JwtProperties(SECRET, 60));
    }

    @Test
    void deveGerarTokenEValidarDeVoltaParaOMesmoUsuario() {
        AuthenticatedUser usuario = new AuthenticatedUser(UUID.randomUUID(), "dra.ana", SecurityRole.MEDICO);

        String token = jwtService.gerarToken(usuario);
        Optional<AuthenticatedUser> resultado = jwtService.validarToken(token);

        assertThat(resultado).contains(usuario);
    }

    @Test
    void deveRetornarVazioParaTokenMalformado() {
        assertThat(jwtService.validarToken("token-invalido")).isEmpty();
    }

    @Test
    void deveRetornarVazioParaTokenAssinadoComOutroSegredo() {
        AuthenticatedUser usuario = new AuthenticatedUser(UUID.randomUUID(), "dra.ana", SecurityRole.MEDICO);
        JwtService outroServico = new JwtService(new JwtProperties("outro-segredo-com-32-caracteres!", 60));
        String token = outroServico.gerarToken(usuario);

        assertThat(jwtService.validarToken(token)).isEmpty();
    }

    @Test
    void deveRetornarVazioParaTokenExpirado() {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        Instant passado = Instant.now().minus(10, ChronoUnit.MINUTES);
        String tokenExpirado = Jwts.builder()
                .subject(UUID.randomUUID().toString())
                .claim("username", "u")
                .claim("role", SecurityRole.PACIENTE.name())
                .issuedAt(Date.from(passado.minus(1, ChronoUnit.MINUTES)))
                .expiration(Date.from(passado))
                .signWith(key)
                .compact();

        assertThat(jwtService.validarToken(tokenExpirado)).isEmpty();
    }

    @Test
    void devePreservarPapelDoUsuarioNoToken() {
        AuthenticatedUser paciente = new AuthenticatedUser(UUID.randomUUID(), "paciente.joao", SecurityRole.PACIENTE);
        String token = jwtService.gerarToken(paciente);

        Optional<AuthenticatedUser> resultado = jwtService.validarToken(token);

        assertThat(resultado).map(AuthenticatedUser::role).contains(SecurityRole.PACIENTE);
    }
}
