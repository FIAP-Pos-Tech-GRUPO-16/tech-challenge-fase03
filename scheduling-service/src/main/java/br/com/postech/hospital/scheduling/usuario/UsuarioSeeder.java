package br.com.postech.hospital.scheduling.usuario;

import br.com.postech.hospital.security.SecurityRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Popula usuários de demonstração (um por papel) na primeira subida do serviço, para permitir
 * login imediato via Postman/Swagger sem um fluxo de cadastro — que não faz parte do escopo
 * do desafio. Não faz nada se já existir algum usuário, então é seguro reiniciar o serviço
 * quantas vezes for preciso.
 */
@Component
public class UsuarioSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(UsuarioSeeder.class);
    private static final String SENHA_PADRAO = "Senha@123";

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioSeeder(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (usuarioRepository.count() > 0) {
            return;
        }

        List<Usuario> usuariosDemonstracao = List.of(
                Usuario.novo("Dra. Ana Souza", "medica.ana", passwordEncoder.encode(SENHA_PADRAO), SecurityRole.MEDICO),
                Usuario.novo("Enf. Bruno Lima", "enfermeiro.bruno", passwordEncoder.encode(SENHA_PADRAO), SecurityRole.ENFERMEIRO),
                Usuario.novo("João Pereira", "paciente.joao", passwordEncoder.encode(SENHA_PADRAO), SecurityRole.PACIENTE),
                Usuario.novo("Maria Santos", "paciente.maria", passwordEncoder.encode(SENHA_PADRAO), SecurityRole.PACIENTE)
        );

        usuarioRepository.saveAll(usuariosDemonstracao);
        log.info("Usuários de demonstração criados: {}", usuariosDemonstracao.size());
    }
}
