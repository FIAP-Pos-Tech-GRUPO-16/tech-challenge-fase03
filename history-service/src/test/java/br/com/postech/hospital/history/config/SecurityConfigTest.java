package br.com.postech.hospital.history.config;

import br.com.postech.hospital.history.graphql.HistoricoGraphQlController;
import br.com.postech.hospital.history.historico.HistoricoService;
import br.com.postech.hospital.security.AuthenticatedUser;
import br.com.postech.hospital.security.JwtService;
import br.com.postech.hospital.security.SecurityRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HistoricoGraphQlController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
        "security.jwt.secret=segredo-exclusivo-de-teste-com-mais-de-32-caracteres",
        "security.jwt.expiration-minutes=60"
})
@DisplayName("Política de acesso do serviço de histórico")
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private HistoricoService historicoService;

    @Test
    @DisplayName("/graphql sem token -> 401")
    void graphqlSemTokenDeveRetornar401() throws Exception {
        mockMvc.perform(get("/graphql")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("/graphql com token malformado -> 401")
    void graphqlComTokenMalformadoDeveRetornar401() throws Exception {
        mockMvc.perform(get("/graphql").header(AUTHORIZATION, "Bearer nao.e.um.jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("/graphql sem o prefixo Bearer -> 401")
    void graphqlSemPrefixoBearerDeveRetornar401() throws Exception {
        String token = jwtService.gerarToken(
                new AuthenticatedUser(UUID.randomUUID(), "medica.ana", SecurityRole.MEDICO));

        mockMvc.perform(get("/graphql").header(AUTHORIZATION, token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GraphiQL é liberado — é interface, não dado")
    void graphiqlNaoDeveExigirToken() throws Exception {
        int status = mockMvc.perform(get("/graphiql")).andReturn().getResponse().getStatus();

        assertThat(status).isNotEqualTo(401);
    }

    @Test
    @DisplayName("valida o token emitido pelo agendamento, sem tabela de usuários própria")
    void deveAceitarTokenEmitidoPeloAgendamento() {
        String token = jwtService.gerarToken(
                new AuthenticatedUser(UUID.randomUUID(), "medica.ana", SecurityRole.MEDICO));

        assertThat(jwtService.validarToken(token)).isPresent();
        assertThat(jwtService.validarToken(token).orElseThrow().role()).isEqualTo(SecurityRole.MEDICO);
    }

    @Test
    @DisplayName("token assinado com outro segredo é recusado")
    void tokenDeOutroEmissorDeveSerRecusado() {
        JwtService emissorIntruso = new JwtService(
                new br.com.postech.hospital.security.JwtProperties("segredo-de-emissor-nao-confiavel-com-32+", 60));
        String tokenIntruso = emissorIntruso.gerarToken(
                new AuthenticatedUser(UUID.randomUUID(), "falso.medico", SecurityRole.MEDICO));

        assertThat(jwtService.validarToken(tokenIntruso)).isEmpty();
    }
}
