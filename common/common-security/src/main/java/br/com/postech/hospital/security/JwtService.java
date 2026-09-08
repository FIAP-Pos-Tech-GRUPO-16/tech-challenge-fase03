package br.com.postech.hospital.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

/**
 * Emite e valida os JWT usados como mecanismo de autenticação stateless entre os serviços.
 * O subject do token é o id do usuário (não o username), pois é esse id que o restante do
 * domínio usa como {@code pacienteId} / {@code medicoId}.
 */
public class JwtService {

    private static final String CLAIM_USERNAME = "username";
    private static final String CLAIM_ROLE = "role";

    private final SecretKey key;
    private final Duration expiration;

    public JwtService(JwtProperties properties) {
        this.key = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
        this.expiration = Duration.ofMinutes(properties.expirationMinutes());
    }

    public String gerarToken(AuthenticatedUser usuario) {
        Instant agora = Instant.now();
        return Jwts.builder()
                .subject(usuario.id().toString())
                .claim(CLAIM_USERNAME, usuario.username())
                .claim(CLAIM_ROLE, usuario.role().name())
                .issuedAt(Date.from(agora))
                .expiration(Date.from(agora.plus(expiration)))
                .signWith(key)
                .compact();
    }

    public Duration expiracao() {
        return expiration;
    }

    /**
     * Valida a assinatura e a expiração do token e reconstrói o usuário autenticado.
     * Retorna {@link Optional#empty()} para qualquer token ausente, expirado, malformado
     * ou com assinatura inválida — o chamador (o filtro de autenticação) decide o que fazer
     * com uma requisição não autenticada, este serviço não lança exceção de segurança.
     */
    public Optional<AuthenticatedUser> validarToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            UUID id = UUID.fromString(claims.getSubject());
            String username = claims.get(CLAIM_USERNAME, String.class);
            SecurityRole role = SecurityRole.valueOf(claims.get(CLAIM_ROLE, String.class));
            return Optional.of(new AuthenticatedUser(id, username, role));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
