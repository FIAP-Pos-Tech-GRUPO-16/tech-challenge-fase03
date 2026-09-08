package br.com.postech.hospital.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Segredo e tempo de expiração do JWT, compartilhados pelos três serviços via a mesma
 * variável de ambiente ({@code SECURITY_JWT_SECRET}) — é o que permite que notificação e
 * histórico validem tokens emitidos pelo agendamento sem chamá-lo a cada requisição.
 *
 * @param secret             chave HMAC (mínimo 256 bits / 32 caracteres) usada para assinar e validar os tokens
 * @param expirationMinutes  tempo de validade do token, em minutos
 */
@ConfigurationProperties(prefix = "security.jwt")
public record JwtProperties(String secret, long expirationMinutes) {

    public JwtProperties {
        if (secret == null || secret.getBytes().length < 32) {
            throw new IllegalArgumentException("security.jwt.secret deve ter ao menos 32 caracteres (256 bits)");
        }
        if (expirationMinutes <= 0) {
            throw new IllegalArgumentException("security.jwt.expiration-minutes deve ser positivo");
        }
    }
}
