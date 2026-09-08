package br.com.postech.hospital.history;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

/**
 * Exclui a auto-configuração de usuário em memória do Spring Boot: este serviço nunca
 * autentica usuário/senha localmente, apenas valida o JWT emitido pelo agendamento.
 */
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class HistoryServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(HistoryServiceApplication.class, args);
    }
}
