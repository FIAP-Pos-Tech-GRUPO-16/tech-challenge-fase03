package br.com.postech.hospital.history.graphql;

import br.com.postech.hospital.history.historico.ConsultaHistoricoResponse;
import br.com.postech.hospital.history.historico.HistoricoService;
import br.com.postech.hospital.security.AuthenticatedUser;
import br.com.postech.hospital.security.SecurityRole;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.GraphQlTest;
import org.springframework.context.annotation.Import;
import org.springframework.graphql.execution.GraphQlSource;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@GraphQlTest(HistoricoGraphQlController.class)
@Import(GraphQlExceptionResolver.class)
@DisplayName("Schema GraphQL do historico")
class HistoricoGraphQlSchemaTest {

    @Autowired
    private GraphQlTester graphQlTester;

    @Autowired
    private GraphQlSource graphQlSource;

    @MockitoBean
    private HistoricoService historicoService;

    private final UUID pacienteId = UUID.randomUUID();
    private final UUID outroPacienteId = UUID.randomUUID();

    @AfterEach
    void limparContexto() {
        SecurityContextHolder.clearContext();
    }

    private void autenticarComo(SecurityRole papel, UUID id) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        new AuthenticatedUser(id, "usuario-" + papel, papel), null, List.of()));
    }

    private ConsultaHistoricoResponse consultaDoHistorico() {
        return new ConsultaHistoricoResponse(
                UUID.randomUUID(), pacienteId, UUID.randomUUID(),
                "2030-01-15T10:30", "AGENDADA", "2026-09-13T08:00");
    }

    @Test
    @DisplayName("o schema declara exatamente as quatro queries previstas no enunciado")
    void schemaDeveDeclararAsQueriesEsperadas() {
        GraphQLSchema schema = graphQlSource.schema();
        GraphQLObjectType query = schema.getQueryType();

        assertThat(query.getFieldDefinitions())
                .extracting(GraphQLFieldDefinition::getName)
                .containsExactlyInAnyOrder(
                        "consultasPorPaciente",
                        "consultasFuturasPorPaciente",
                        "minhasConsultas",
                        "minhasConsultasFuturas");
    }

    @Test
    @DisplayName("consultasPorPaciente devolve todos os campos declarados no tipo Consulta")
    void consultasPorPacienteDeveResolverTodosOsCampos() {
        autenticarComo(SecurityRole.MEDICO, UUID.randomUUID());
        when(historicoService.consultasDoPaciente(eq(pacienteId), any()))
                .thenReturn(List.of(consultaDoHistorico()));

        graphQlTester.document("""
                        query($pacienteId: ID!) {
                          consultasPorPaciente(pacienteId: $pacienteId) {
                            id pacienteId medicoId dataHora status atualizadoEm
                          }
                        }
                        """)
                .variable("pacienteId", pacienteId.toString())
                .execute()
                .path("consultasPorPaciente")
                .entityList(Object.class).hasSize(1);
    }

    @Test
    @DisplayName("consultasFuturasPorPaciente resolve e chama o filtro de futuras")
    void consultasFuturasPorPacienteDeveResolver() {
        autenticarComo(SecurityRole.ENFERMEIRO, UUID.randomUUID());
        when(historicoService.consultasFuturasDoPaciente(eq(pacienteId), any()))
                .thenReturn(List.of(consultaDoHistorico()));

        graphQlTester.document("""
                        query($pacienteId: ID!) {
                          consultasFuturasPorPaciente(pacienteId: $pacienteId) { id status }
                        }
                        """)
                .variable("pacienteId", pacienteId.toString())
                .execute()
                .path("consultasFuturasPorPaciente")
                .entityList(Object.class).hasSize(1);
    }

    @Test
    @DisplayName("minhasConsultas usa a identidade do token")
    void minhasConsultasDeveUsarOPacienteAutenticado() {
        autenticarComo(SecurityRole.PACIENTE, pacienteId);
        when(historicoService.consultasDoPaciente(eq(pacienteId), any()))
                .thenReturn(List.of(consultaDoHistorico()));

        graphQlTester.document("{ minhasConsultas { id pacienteId } }")
                .execute()
                .path("minhasConsultas")
                .entityList(Object.class).hasSize(1);
    }

    @Test
    @DisplayName("minhasConsultasFuturas usa a identidade do token")
    void minhasConsultasFuturasDeveUsarOPacienteAutenticado() {
        autenticarComo(SecurityRole.PACIENTE, pacienteId);
        when(historicoService.consultasFuturasDoPaciente(eq(pacienteId), any()))
                .thenReturn(List.of(consultaDoHistorico()));

        graphQlTester.document("{ minhasConsultasFuturas { id dataHora } }")
                .execute()
                .path("minhasConsultasFuturas")
                .entityList(Object.class).hasSize(1);
    }

    @Test
    @DisplayName("minhasConsultas para um medico -> erro FORBIDDEN")
    void minhasConsultasDeveSerProibidoParaMedico() {
        autenticarComo(SecurityRole.MEDICO, UUID.randomUUID());

        graphQlTester.document("{ minhasConsultas { id } }")
                .execute()
                .errors()
                .expect(erro -> "FORBIDDEN".equals(String.valueOf(erro.getErrorType())))
                .verify();
    }

    @Test
    @DisplayName("paciente consultando o historico de outro paciente -> erro FORBIDDEN")
    void pacienteNaoPodeConsultarHistoricoDeOutroPaciente() {
        autenticarComo(SecurityRole.PACIENTE, pacienteId);
        when(historicoService.consultasDoPaciente(eq(outroPacienteId), any()))
                .thenThrow(new org.springframework.security.access.AccessDeniedException(
                        "Paciente so pode consultar o proprio historico"));

        graphQlTester.document("""
                        query($pacienteId: ID!) {
                          consultasPorPaciente(pacienteId: $pacienteId) { id }
                        }
                        """)
                .variable("pacienteId", outroPacienteId.toString())
                .execute()
                .errors()
                .expect(erro -> "FORBIDDEN".equals(String.valueOf(erro.getErrorType())))
                .verify();
    }
}
