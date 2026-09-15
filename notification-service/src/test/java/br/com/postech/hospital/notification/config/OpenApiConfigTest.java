package br.com.postech.hospital.notification.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sem o esquema bearer declarado, o botão "Authorize" do Swagger não aparece e fica impossível
 * exercitar as rotas protegidas pela própria documentação interativa — que é o caminho descrito
 * no README.
 */
@DisplayName("Documentação OpenAPI do serviço de notificações")
class OpenApiConfigTest {

    @Test
    @DisplayName("declara o esquema bearerAuth com formato JWT")
    void deveDeclararEsquemaBearerJwt() {
        OpenAPI openApi = new OpenApiConfig().notificationServiceOpenApi();

        assertThat(openApi.getInfo().getTitle()).isNotBlank();
        assertThat(openApi.getSecurity()).isNotEmpty();

        SecurityScheme esquema = openApi.getComponents().getSecuritySchemes().get("bearerAuth");
        assertThat(esquema).isNotNull();
        assertThat(esquema.getType()).isEqualTo(SecurityScheme.Type.HTTP);
        assertThat(esquema.getScheme()).isEqualTo("bearer");
        assertThat(esquema.getBearerFormat()).isEqualTo("JWT");
    }
}
