package br.com.postech.hospital.scheduling.auth;

import br.com.postech.hospital.security.SecurityRole;

public record LoginResponse(String token, String tipo, SecurityRole papel, long expiraEmMinutos) {
}
