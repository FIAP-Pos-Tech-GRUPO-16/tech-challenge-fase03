package br.com.postech.hospital.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Objects;
import java.util.UUID;

/**
 * Identidade do usuário autenticado, extraída do JWT pelo {@link JwtAuthenticationFilter} e
 * usada como principal da {@link Authentication} durante toda a requisição.
 *
 * <p>{@code id} é o identificador do usuário na tabela de usuários do serviço de agendamento —
 * quando o papel é {@link SecurityRole#PACIENTE}, esse mesmo id é o {@code pacienteId} usado
 * para checagens de propriedade dos dados (um paciente só pode ver as próprias consultas).
 */
public record AuthenticatedUser(UUID id, String username, SecurityRole role) {

    public AuthenticatedUser {
        Objects.requireNonNull(id, "id é obrigatório");
        Objects.requireNonNull(username, "username é obrigatório");
        Objects.requireNonNull(role, "role é obrigatório");
    }

    public boolean isPaciente() {
        return role == SecurityRole.PACIENTE;
    }

    /**
     * Recupera o usuário autenticado do contexto de segurança da requisição atual.
     * Lança {@link IllegalStateException} se não houver autenticação — o que só pode
     * acontecer se este método for chamado fora de uma rota protegida pelo Spring Security,
     * o que é considerado um erro de programação, não um cenário de negócio esperado.
     */
    public static AuthenticatedUser current() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser usuario)) {
            throw new IllegalStateException("Nenhum usuário autenticado no contexto de segurança");
        }
        return usuario;
    }
}
