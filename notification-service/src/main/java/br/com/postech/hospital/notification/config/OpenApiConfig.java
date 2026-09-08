package br.com.postech.hospital.notification.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Documentação interativa da API em {@code /swagger-ui.html}. O token usado aqui é o mesmo
 * emitido pelo login no scheduling-service — este serviço não tem login próprio.
 */
@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI notificationServiceOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Notificações — Sistema Hospitalar")
                        .description("""
                                Consulta dos lembretes enviados aos pacientes sobre suas consultas.

                                O token usado aqui é o mesmo obtido no login do serviço de
                                agendamento (POST /auth/login em :8081). Clique em "Authorize" e
                                cole o token (sem o prefixo "Bearer").
                                """)
                        .version("v1")
                        .contact(new Contact().name("Tech Challenge Fase 03 - Grupo 16")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME))
                .components(new Components().addSecuritySchemes(BEARER_SCHEME,
                        new SecurityScheme()
                                .name(BEARER_SCHEME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
