package br.com.postech.hospital.notification.notificacao;

import br.com.postech.hospital.notification.config.SecurityConfig;
import br.com.postech.hospital.security.AuthenticatedUser;
import br.com.postech.hospital.security.JwtProperties;
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

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NotificacaoController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
        "security.jwt.secret=segredo-exclusivo-de-teste-com-mais-de-32-caracteres",
        "security.jwt.expiration-minutes=60"
})
@DisplayName("Autenticacao do servico de notificacoes")
class NotificacaoSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private NotificacaoService notificacaoService;

    private String tokenDe(SecurityRole papel, UUID id) {
        return "Bearer " + jwtService.gerarToken(new AuthenticatedUser(id, "usuario-" + papel, papel));
    }

    @Test
    @DisplayName("sem token -> 401")
    void semTokenDeveRetornar401() throws Exception {
        mockMvc.perform(get("/notificacoes"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("token assinado com outro segredo -> 401")
    void tokenDeOutroEmissorDeveRetornar401() throws Exception {
        JwtService emissorIntruso = new JwtService(
                new JwtProperties("segredo-de-um-emissor-nao-confiavel-com-32+", 60));
        String tokenIntruso = emissorIntruso.gerarToken(
                new AuthenticatedUser(UUID.randomUUID(), "falso.paciente", SecurityRole.PACIENTE));

        mockMvc.perform(get("/notificacoes").header(AUTHORIZATION, "Bearer " + tokenIntruso))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("token valido de paciente -> 200")
    void pacienteAutenticadoPodeListarSeusLembretes() throws Exception {
        UUID pacienteId = UUID.randomUUID();
        when(notificacaoService.listar(any(), any())).thenReturn(List.of());

        mockMvc.perform(get("/notificacoes")
                        .header(AUTHORIZATION, tokenDe(SecurityRole.PACIENTE, pacienteId)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("token valido de enfermeiro com filtro por paciente -> 200")
    void enfermeiroPodeFiltrarPorPaciente() throws Exception {
        UUID pacienteId = UUID.randomUUID();
        when(notificacaoService.listar(eq(pacienteId), any())).thenReturn(List.of());

        mockMvc.perform(get("/notificacoes")
                        .param("pacienteId", pacienteId.toString())
                        .header(AUTHORIZATION, tokenDe(SecurityRole.ENFERMEIRO, UUID.randomUUID())))
                .andExpect(status().isOk());
    }
}
