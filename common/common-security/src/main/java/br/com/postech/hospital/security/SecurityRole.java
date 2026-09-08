package br.com.postech.hospital.security;

/** Perfis de acesso do sistema hospitalar, com permissões definidas no enunciado do desafio. */
public enum SecurityRole {
    MEDICO,
    ENFERMEIRO,
    PACIENTE;

    public String authority() {
        return "ROLE_" + name();
    }
}
