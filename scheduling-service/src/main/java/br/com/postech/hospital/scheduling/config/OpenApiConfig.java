package br.com.postech.hospital.scheduling.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Documentação interativa da API em {@code /swagger-ui.html}. Define o esquema "bearerAuth"
 * para que seja possível colar o token retornado pelo login e testar as rotas protegidas
 * diretamente pela interface do Swagger, sem precisar de um cliente HTTP externo.
 */
@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI schedulingServiceOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Agendamento — Sistema Hospitalar")
                        .description("""
                                Cadastro de usuários, login e agendamento de consultas.

                                Para testar as rotas protegidas: faça login em POST /auth/login,
                                copie o campo "token" da resposta e clique em "Authorize" no topo
                                desta página, colando o token (sem o prefixo "Bearer").
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
