package br.com.postech.hospital.scheduling.consulta;

import br.com.postech.hospital.events.StatusConsulta;
import br.com.postech.hospital.scheduling.usuario.Usuario;
import br.com.postech.hospital.scheduling.usuario.UsuarioRepository;
import br.com.postech.hospital.security.AuthenticatedUser;
import br.com.postech.hospital.security.JwtProperties;
import br.com.postech.hospital.security.JwtService;
import br.com.postech.hospital.security.SecurityRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Regras de acesso do servico de agendamento (ponta a ponta)")
class ConsultaSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ConsultaRepository consultaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    /**
     * Substitui a publicacao real no broker; a publicacao em si e coberta por ConsultaEventPublisherTest.
     */
    @MockitoBean
    private RabbitTemplate rabbitTemplate;

    private final UUID pacienteId = UUID.randomUUID();
    private final UUID outroPacienteId = UUID.randomUUID();
    private final UUID medicoId = UUID.randomUUID();
    private final UUID enfermeiroId = UUID.randomUUID();

    @BeforeEach
    void prepararBase() {
        consultaRepository.deleteAll();
        usuarioRepository.deleteAll();

        // a criacao de consulta passou a exigir que paciente e medico existam com o papel correto
        usuarioRepository.save(new Usuario(pacienteId, "Joao Pereira", "paciente.joao", "hash", SecurityRole.PACIENTE));
        usuarioRepository.save(new Usuario(outroPacienteId, "Maria Santos", "paciente.maria", "hash", SecurityRole.PACIENTE));
        usuarioRepository.save(new Usuario(medicoId, "Dra. Ana Souza", "medica.ana", "hash", SecurityRole.MEDICO));
        usuarioRepository.save(new Usuario(enfermeiroId, "Enf. Bruno Lima", "enfermeiro.bruno", "hash", SecurityRole.ENFERMEIRO));
    }

    private String tokenDe(SecurityRole papel, UUID id) {
        return "Bearer " + jwtService.gerarToken(new AuthenticatedUser(id, "usuario-" + papel, papel));
    }

    private String tokenMedico() {
        return tokenDe(SecurityRole.MEDICO, medicoId);
    }

    private String tokenEnfermeiro() {
        return tokenDe(SecurityRole.ENFERMEIRO, enfermeiroId);
    }

    private String tokenPaciente() {
        return tokenDe(SecurityRole.PACIENTE, pacienteId);
    }

    private String bodyConsulta(LocalDateTime dataHora) {
        return """
                {"pacienteId":"%s","medicoId":"%s","dataHora":"%s","observacoes":"consulta de rotina"}
                """.formatted(pacienteId, medicoId, dataHora);
    }

    private Consulta consultaSalvaDe(UUID donoPacienteId) {
        return consultaRepository.save(
                Consulta.agendar(donoPacienteId, medicoId, enfermeiroId,
                        LocalDateTime.now().plusDays(7), "agendada para o teste"));
    }

    @Test
    @DisplayName("sem token -> 401")
    void semTokenDeveRetornar401() throws Exception {
        mockMvc.perform(get("/consultas"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("token malformado -> 401")
    void tokenMalformadoDeveRetornar401() throws Exception {
        mockMvc.perform(get("/consultas").header(AUTHORIZATION, "Bearer nao.e.um.jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("token assinado com outro segredo -> 401")
    void tokenDeOutroEmissorDeveRetornar401() throws Exception {
        JwtService emissorIntruso = new JwtService(
                new JwtProperties("segredo-de-um-emissor-nao-confiavel-com-32+", 60));
        String tokenIntruso = emissorIntruso.gerarToken(
                new AuthenticatedUser(medicoId, "falso.medico", SecurityRole.MEDICO));

        mockMvc.perform(get("/consultas").header(AUTHORIZATION, "Bearer " + tokenIntruso))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("enfermeiro registra consulta -> 201 (requisito: enfermeiros podem registrar consultas)")
    void enfermeiroPodeCriarConsulta() throws Exception {
        mockMvc.perform(post("/consultas")
                        .header(AUTHORIZATION, tokenEnfermeiro())
                        .contentType(APPLICATION_JSON)
                        .content(bodyConsulta(LocalDateTime.now().plusDays(3))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(StatusConsulta.AGENDADA.name()))
                .andExpect(jsonPath("$.pacienteId").value(pacienteId.toString()))
                .andExpect(jsonPath("$.registradaPorId").value(enfermeiroId.toString()));
    }

    @Test
    @DisplayName("medico registra consulta -> 201")
    void medicoPodeCriarConsulta() throws Exception {
        mockMvc.perform(post("/consultas")
                        .header(AUTHORIZATION, tokenMedico())
                        .contentType(APPLICATION_JSON)
                        .content(bodyConsulta(LocalDateTime.now().plusDays(3))))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("paciente tenta registrar consulta -> 403")
    void pacienteNaoPodeCriarConsulta() throws Exception {
        mockMvc.perform(post("/consultas")
                        .header(AUTHORIZATION, tokenPaciente())
                        .contentType(APPLICATION_JSON)
                        .content(bodyConsulta(LocalDateTime.now().plusDays(3))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("data no passado -> 400 (Bean Validation + GlobalExceptionHandler)")
    void dataNoPassadoDeveRetornar400() throws Exception {
        mockMvc.perform(post("/consultas")
                        .header(AUTHORIZATION, tokenEnfermeiro())
                        .contentType(APPLICATION_JSON)
                        .content(bodyConsulta(LocalDateTime.now().minusDays(1))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detalhes[0]").value(containsString("dataHora")));
    }

    @Test
    @DisplayName("medicoId inexistente -> 400, e nao 500 (regressao: a FK estourava como erro do servidor)")
    void medicoInexistenteDeveRetornar400() throws Exception {
        String corpoComMedicoFantasma = """
                {"pacienteId":"%s","medicoId":"%s","dataHora":"%s"}
                """.formatted(pacienteId, UUID.randomUUID(), LocalDateTime.now().plusDays(3));

        mockMvc.perform(post("/consultas")
                        .header(AUTHORIZATION, tokenEnfermeiro())
                        .contentType(APPLICATION_JSON)
                        .content(corpoComMedicoFantasma))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("agendar com um paciente no lugar do medico -> 400")
    void medicoQueNaVerdadeEhPacienteDeveRetornar400() throws Exception {
        String corpoComPapelTrocado = """
                {"pacienteId":"%s","medicoId":"%s","dataHora":"%s"}
                """.formatted(pacienteId, outroPacienteId, LocalDateTime.now().plusDays(3));

        mockMvc.perform(post("/consultas")
                        .header(AUTHORIZATION, tokenEnfermeiro())
                        .contentType(APPLICATION_JSON)
                        .content(corpoComPapelTrocado))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("medico edita consulta -> 200 (requisito: medicos podem editar)")
    void medicoPodeEditarConsulta() throws Exception {
        Consulta consulta = consultaSalvaDe(pacienteId);

        mockMvc.perform(put("/consultas/" + consulta.getId())
                        .header(AUTHORIZATION, tokenMedico())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"dataHora":"%s","status":"REALIZADA","observacoes":"paciente compareceu"}
                                """.formatted(LocalDateTime.now().plusDays(8))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(StatusConsulta.REALIZADA.name()));
    }

    @Test
    @DisplayName("a edicao e mesmo gravada no banco (relendo a linha, nao a resposta HTTP)")
    void edicaoDevePersistirNoBanco() throws Exception {
        Consulta consulta = consultaSalvaDe(pacienteId);
        LocalDateTime novaDataHora = LocalDateTime.now().plusDays(30).withNano(0);

        mockMvc.perform(put("/consultas/" + consulta.getId())
                        .header(AUTHORIZATION, tokenMedico())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"dataHora":"%s","status":"CANCELADA","observacoes":"paciente remarcou"}
                                """.formatted(novaDataHora)))
                .andExpect(status().isOk());

        // relendo do repositorio: se o servico dependesse de um save() que nao existe,
        // a resposta HTTP viria correta (montada do objeto em memoria) e a linha ficaria velha
        Consulta doBanco = consultaRepository.findById(consulta.getId()).orElseThrow();
        assertThat(doBanco.getStatus()).isEqualTo(StatusConsulta.CANCELADA);
        assertThat(doBanco.getObservacoes()).isEqualTo("paciente remarcou");
        assertThat(doBanco.getDataHora()).isEqualTo(novaDataHora);
    }

    @Test
    @DisplayName("paciente tenta editar consulta -> 403")
    void pacienteNaoPodeEditarConsulta() throws Exception {
        Consulta consulta = consultaSalvaDe(pacienteId);

        mockMvc.perform(put("/consultas/" + consulta.getId())
                        .header(AUTHORIZATION, tokenPaciente())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"dataHora":"%s","status":"CANCELADA","observacoes":"quero cancelar"}
                                """.formatted(LocalDateTime.now().plusDays(8))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("editar consulta inexistente -> 404")
    void editarConsultaInexistenteDeveRetornar404() throws Exception {
        mockMvc.perform(put("/consultas/" + UUID.randomUUID())
                        .header(AUTHORIZATION, tokenMedico())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"dataHora":"%s","status":"CANCELADA","observacoes":null}
                                """.formatted(LocalDateTime.now().plusDays(8))))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("paciente busca a propria consulta -> 200")
    void pacientePodeVerAPropriaConsulta() throws Exception {
        Consulta consulta = consultaSalvaDe(pacienteId);

        mockMvc.perform(get("/consultas/" + consulta.getId()).header(AUTHORIZATION, tokenPaciente()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(consulta.getId().toString()));
    }

    @Test
    @DisplayName("paciente busca consulta de outro paciente -> 403 (requisito: pacientes veem apenas as suas)")
    void pacienteNaoPodeVerConsultaDeOutroPaciente() throws Exception {
        Consulta consultaAlheia = consultaSalvaDe(outroPacienteId);

        mockMvc.perform(get("/consultas/" + consultaAlheia.getId()).header(AUTHORIZATION, tokenPaciente()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("medico busca consulta de qualquer paciente -> 200")
    void medicoPodeVerConsultaDeQualquerPaciente() throws Exception {
        Consulta consultaAlheia = consultaSalvaDe(outroPacienteId);

        mockMvc.perform(get("/consultas/" + consultaAlheia.getId()).header(AUTHORIZATION, tokenMedico()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("listagem do paciente ignora o filtro informado e devolve so as dele")
    void listagemDoPacienteIgnoraFiltroEDevolveSomenteAsProprias() throws Exception {
        consultaSalvaDe(pacienteId);
        consultaSalvaDe(outroPacienteId);
        consultaSalvaDe(outroPacienteId);

        // mesmo pedindo explicitamente as consultas de outro paciente, o filtro e sobrescrito
        mockMvc.perform(get("/consultas")
                        .param("pacienteId", outroPacienteId.toString())
                        .header(AUTHORIZATION, tokenPaciente()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].pacienteId").value(pacienteId.toString()));
    }

    @Test
    @DisplayName("equipe clinica lista todas as consultas -> 200")
    void enfermeiroListaTodasAsConsultas() throws Exception {
        consultaSalvaDe(pacienteId);
        consultaSalvaDe(outroPacienteId);

        mockMvc.perform(get("/consultas").header(AUTHORIZATION, tokenEnfermeiro()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }
}
