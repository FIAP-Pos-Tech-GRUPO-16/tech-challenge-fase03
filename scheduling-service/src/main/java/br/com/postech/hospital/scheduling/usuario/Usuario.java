package br.com.postech.hospital.scheduling.usuario;

import br.com.postech.hospital.security.SecurityRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Usuário do sistema hospitalar. O mesmo cadastro serve para os três papéis do domínio —
 * médico, enfermeiro e paciente — diferenciados pelo campo {@code role}, que também define
 * as autoridades do JWT emitido no login.
 */
@Entity
@Table(name = "usuarios")
public class Usuario {

    @Id
    private UUID id;

    @Column(name = "nome_completo", nullable = false)
    private String nomeCompleto;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SecurityRole role;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    protected Usuario() {
    }

    public Usuario(UUID id, String nomeCompleto, String username, String passwordHash, SecurityRole role) {
        this.id = Objects.requireNonNull(id, "id é obrigatório");
        this.nomeCompleto = Objects.requireNonNull(nomeCompleto, "nomeCompleto é obrigatório");
        this.username = Objects.requireNonNull(username, "username é obrigatório");
        this.passwordHash = Objects.requireNonNull(passwordHash, "passwordHash é obrigatório");
        this.role = Objects.requireNonNull(role, "role é obrigatório");
        this.criadoEm = LocalDateTime.now();
    }

    public static Usuario novo(String nomeCompleto, String username, String passwordHash, SecurityRole role) {
        return new Usuario(UUID.randomUUID(), nomeCompleto, username, passwordHash, role);
    }

    public UUID getId() {
        return id;
    }

    public String getNomeCompleto() {
        return nomeCompleto;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public SecurityRole getRole() {
        return role;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Usuario usuario)) return false;
        return Objects.equals(id, usuario.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
